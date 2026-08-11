package com.vos.accounting.data

import com.vos.accounting.model.MAX_AMOUNT_MINOR
import com.vos.accounting.model.MAX_RATE_TO_CNY_SCALED
import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.model.AutoBookkeepingStatus
import com.vos.accounting.model.NotificationPrivacyMode
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.OverviewTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * 表示用户可修正的账务写入错误。
 */
class AccountingWriteException(message: String) : IllegalArgumentException(message)

/** 承载通知展示所需的自动账单与本地分类、账户名称。 */
data class AutoBookkeepingNotificationData(
    val event: AutoBookkeepingEventEntity,
    val accountName: String?,
    val categoryName: String?,
)

/**
 * 汇总账本数据读取与统一写入流程。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountingRepository(
    private val dao: AccountingDao,
) {
    val accounts = dao.observeAccounts()
    val ledgers = dao.observeLedgers()
    val accountLedgerCrossRefs = dao.observeAccountLedgerCrossRefs()
    val accountTypes = dao.observeAccountTypes()
    val currencies = dao.observeCurrencies()
    val categories = dao.observeCategories()
    val settings = dao.observeSettings()
    val pendingAutoBookkeepingEvents = dao.observePendingAutoBookkeepingEvents()
    private val currentLedgerId: Flow<Long> = settings
        .map { it?.currentLedgerId ?: 1L }
        .distinctUntilChanged()
    private val storedTransactions = currentLedgerId.flatMapLatest { dao.observeTransactions(it) }
    val transactions = combine(storedTransactions, ledgers, currencies, ::convertBaseAmounts)
    val accountBalances = dao.observeAccountBalances()
        .map { records -> records.associateBy(AccountBalanceRecord::accountId) }
        .distinctUntilChanged()
    val accountExchangeLinks = dao.observeAccountExchangeLinks().distinctUntilChanged()
    val overviewTotals = transactions.map { records ->
        val reportRecords = records.filter { it.exchangeId == null }
        OverviewTotals(
            incomeMinor = reportRecords.filter { it.type == TransactionType.INCOME }.sumOf(TransactionRecord::baseAmountMinor),
            expenseMinor = reportRecords.filter { it.type == TransactionType.EXPENSE }.sumOf(TransactionRecord::baseAmountMinor),
        )
    }

    /**
     * 建立首次启动所需的默认账本数据。
     */
    suspend fun initialize() {
        dao.seedDefaults()
        dao.deleteProcessedAutoBookkeepingEventsBefore(
            System.currentTimeMillis() - AUTO_EVENT_RETENTION_MILLIS,
        )
    }

    /**
     * 仅刷新实际使用中、开启自动汇率且超过缓存有效期的非人民币币种；失败时保留本地值。
     */
    suspend fun refreshBuiltinCurrencyRates() {
        val threshold = System.currentTimeMillis() - RATE_REFRESH_TTL_MILLIS
        val usedKeys = dao.usedCurrencyKeys().toSet()
        val targets = currencies.first().filter {
            it.isBuiltin && it.autoRateEnabled && it.code != "CNY" &&
                it.key in usedKeys && it.updatedAt < threshold
        }
        if (targets.isEmpty()) return
        val rates = withContext(Dispatchers.IO) {
            fetchCurrencyRates(targets.map(CurrencyEntity::code).toSet())
        }
        val updatedAt = System.currentTimeMillis()
        rates.forEach { (code, rate) ->
            dao.updateBuiltinCurrencyRate(code, rate, updatedAt)
        }
    }

    /**
     * 更新应用外观的明暗模式。
     */
    suspend fun updateThemeMode(themeMode: String) {
        dao.updateThemeMode(themeMode)
    }

    /**
     * 更新应用是否跟随系统配色。
     */
    suspend fun updateFollowSystemColor(followSystemColor: Boolean) {
        dao.updateFollowSystemColor(followSystemColor)
    }

    /**
     * 更新应用是否启用预测性返回动画。
     */
    suspend fun updatePredictiveBackAnimationEnabled(enabled: Boolean) {
        dao.updatePredictiveBackAnimationEnabled(enabled)
    }

    /** 更新普通收支金额是否使用红绿字体。 */
    suspend fun updateColoredTransactionAmountsEnabled(enabled: Boolean) {
        dao.updateColoredTransactionAmountsEnabled(enabled)
    }

    /** 更新自动记账总开关。 */
    suspend fun updateAutoBookkeepingEnabled(enabled: Boolean) {
        dao.updateAutoBookkeepingEnabled(enabled)
    }

    /** 更新指定支付平台的自动账单来源开关。 */
    suspend fun updateAutoBookkeepingProviderEnabled(provider: PaymentProvider, enabled: Boolean) {
        when (provider) {
            PaymentProvider.WECHAT -> dao.updateAutoBookkeepingWechatEnabled(enabled)
            PaymentProvider.ALIPAY -> dao.updateAutoBookkeepingAlipayEnabled(enabled)
            PaymentProvider.UNIONPAY -> dao.updateAutoBookkeepingUnionPayEnabled(enabled)
        }
    }

    /** 更新账单通知的隐私展示方式。 */
    suspend fun updateNotificationPrivacyMode(mode: NotificationPrivacyMode) {
        dao.updateNotificationPrivacyMode(mode)
    }

    /** 返回当前应用设置快照。 */
    suspend fun getSettingsSnapshot(): AppSettingsEntity = dao.getSettings() ?: AppSettingsEntity()

    /** 把支付页面或通知提取结果去重、匹配并保存为待确认账单。 */
    suspend fun captureAutoBookkeeping(capture: AutoBookkeepingCapture): AutoBookkeepingEventEntity? {
        val settings = dao.getSettings() ?: AppSettingsEntity()
        if (!settings.autoBookkeepingEnabled || !settings.isProviderEnabled(capture.provider)) return null
        dao.deleteProcessedAutoBookkeepingEventsBefore(
            System.currentTimeMillis() - AUTO_EVENT_RETENTION_MILLIS,
        )
        val merchantKey = normalizeAutoKey(capture.merchant)
        val paymentMethodKey = normalizeAutoKey(capture.paymentMethodKey)
        val fingerprint = autoFingerprint(capture, merchantKey, paymentMethodKey)
        val existing = capture.externalKeyHash?.let { hash ->
            dao.findAutoBookkeepingEventByExternalKey(hash)
        }
            ?: dao.findAutoBookkeepingEventByFingerprint(
                provider = capture.provider,
                fingerprint = fingerprint,
                occurredAt = capture.occurredAt,
                fromTime = capture.occurredAt - AUTO_DUPLICATE_WINDOW_MILLIS,
                toTime = capture.occurredAt + AUTO_DUPLICATE_WINDOW_MILLIS,
        )
        if (existing != null && existing.status != AutoBookkeepingStatus.PENDING) return null
        val eventLedgerId = existing?.ledgerId ?: settings.currentLedgerId
        val prediction = predictAutoBookkeeping(
            capture = capture,
            ledgerId = eventLedgerId,
            merchantKey = merchantKey,
            paymentMethodKey = paymentMethodKey,
        )
        val now = System.currentTimeMillis()
        val mergedSources = ((existing?.captureSources?.split(',') ?: emptyList()) + capture.source.name)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(",")
        val event = AutoBookkeepingEventEntity(
            id = existing?.id ?: 0,
            provider = capture.provider,
            status = AutoBookkeepingStatus.PENDING,
            type = capture.type,
            amountMinor = capture.amountMinor,
            currencyKey = capture.currencyKey,
            accountId = existing?.accountId ?: prediction.accountId,
            categoryId = existing?.categoryId ?: prediction.categoryId,
            merchant = capture.merchant.trim().ifEmpty { existing?.merchant.orEmpty() },
            note = capture.note.trim().ifEmpty { existing?.note.orEmpty() },
            occurredAt = minOf(existing?.occurredAt ?: capture.occurredAt, capture.occurredAt),
            paymentMethodKey = paymentMethodKey.ifEmpty { existing?.paymentMethodKey.orEmpty() },
            destinationPaymentMethodKey = "",
            externalKeyHash = capture.externalKeyHash ?: existing?.externalKeyHash,
            fingerprint = fingerprint,
            captureSources = mergedSources,
            canConfirm = existing?.canConfirm == true || prediction.canConfirm,
            ledgerId = eventLedgerId,
            confirmedTransactionId = null,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        if (existing != null) {
            dao.updateAutoBookkeepingEvent(event)
            return event
        }
        val insertedId = dao.insertAutoBookkeepingEvent(event)
        if (insertedId > 0) return event.copy(id = insertedId)
        return capture.externalKeyHash?.let { hash ->
            dao.findAutoBookkeepingEventByExternalKey(hash)
        }
    }

    /** 返回指定待确认账单的当前内容。 */
    suspend fun findAutoBookkeepingEvent(eventId: Long): AutoBookkeepingEventEntity? =
        dao.findAutoBookkeepingEvent(eventId)

    /** 返回通知详情所需的账单、账户和分类名称。 */
    suspend fun findAutoBookkeepingNotificationData(
        eventId: Long,
    ): AutoBookkeepingNotificationData? {
        val event = dao.findAutoBookkeepingEvent(eventId) ?: return null
        return AutoBookkeepingNotificationData(
            event = event,
            accountName = event.accountId?.let { id -> dao.findAccount(id)?.name },
            categoryName = event.categoryId?.let { id -> dao.findCategory(id)?.name },
        )
    }

    /** 使用事件已有的高置信度字段一键确认正式入账。 */
    suspend fun confirmAutoBookkeepingEvent(eventId: Long): Long {
        val event = dao.findAutoBookkeepingEvent(eventId)
            ?: throw AccountingWriteException("待确认账单不存在")
        if (!event.canConfirm) throw AccountingWriteException("请先补全账单信息")
        return confirmAutoBookkeepingEvent(eventId, event.toTransactionDraft(), learnMappings = false)
    }

    /** 使用用户编辑后的完整草稿确认入账并学习本地映射。 */
    suspend fun confirmAutoBookkeepingEvent(
        eventId: Long,
        draft: TransactionDraft,
        learnMappings: Boolean = true,
    ): Long {
        val event = dao.findAutoBookkeepingEvent(eventId)
            ?: throw AccountingWriteException("待确认账单不存在")
        if (event.status == AutoBookkeepingStatus.CONFIRMED) {
            return event.confirmedTransactionId ?: 0L
        }
        if (event.status != AutoBookkeepingStatus.PENDING) {
            throw AccountingWriteException("待确认账单已经处理")
        }
        val confirmedDraft = draft.copy(source = TransactionSource.AI, ledgerId = event.ledgerId)
        validateTransactionDraft(confirmedDraft)
        val now = System.currentTimeMillis()
        val updatedEvent = event.copy(
            type = confirmedDraft.type,
            amountMinor = confirmedDraft.amountMinor,
            currencyKey = confirmedDraft.currencyKey,
            accountId = confirmedDraft.accountId,
            categoryId = confirmedDraft.categoryId,
            destinationAmountMinor = null,
            destinationAccountId = null,
            refundOfTransactionId = null,
            merchant = confirmedDraft.merchant.trim(),
            note = confirmedDraft.note.trim(),
            occurredAt = confirmedDraft.occurredAt,
            canConfirm = true,
            updatedAt = now,
        )
        val mappings = if (learnMappings) autoBookkeepingMappings(event, confirmedDraft) else AutoMappings()
        val transactionId = dao.confirmAutoBookkeepingEvent(
            eventId = eventId,
            transactions = transactionEntities(confirmedDraft),
            updatedEvent = updatedEvent,
            categoryMapping = mappings.category,
            accountMappings = mappings.accounts,
        )
        return transactionId
    }

    /** 明确忽略一条待确认账单并保留短期防重记录。 */
    suspend fun ignoreAutoBookkeepingEvent(eventId: Long) {
        dao.ignoreAutoBookkeepingEvent(eventId, System.currentTimeMillis())
    }

    /** 根据用户补全结果生成精确商户分类和支付账户映射。 */
    private fun autoBookkeepingMappings(
        event: AutoBookkeepingEventEntity,
        draft: TransactionDraft,
    ): AutoMappings {
        var categoryMapping: AutoCategoryMappingEntity? = null
        val accountMappings = mutableListOf<AutoAccountMappingEntity>()
        val merchantKey = normalizeAutoKey(event.merchant)
        val categoryId = draft.categoryId
        if (merchantKey.isNotEmpty() && categoryId != null) {
            categoryMapping = AutoCategoryMappingEntity(event.provider, merchantKey, draft.type, categoryId)
        }
        if (event.paymentMethodKey.isNotEmpty()) {
            accountMappings += AutoAccountMappingEntity(event.provider, event.paymentMethodKey, draft.accountId)
        }
        return AutoMappings(categoryMapping, accountMappings)
    }

    /**
     * 把已经确认的草稿写入正式账目。
     */
    suspend fun saveTransaction(draft: TransactionDraft): Long {
        validateTransactionDraft(draft)
        return dao.insertTransactions(transactionEntities(draft))
    }

    /**
     * 使用确认后的草稿更新指定账目。
     */
    suspend fun updateTransaction(
        transactionId: Long,
        draft: TransactionDraft,
    ) {
        val original = dao.findTransaction(transactionId)
            ?: throw AccountingWriteException("账目不存在或已被删除")
        validateTransactionDraft(draft, original)
        val snapshot = transactionSnapshot(draft)
        val updated = dao.updateTransaction(
            TransactionEntity(
                id = transactionId,
                type = draft.type,
                amountMinor = draft.amountMinor,
                accountAmountMinor = snapshot.accountAmountMinor,
                accountId = draft.accountId,
                categoryId = draft.categoryId,
                merchant = draft.merchant.trim(),
                note = draft.note.trim(),
                occurredAt = draft.occurredAt,
                source = draft.source,
                ledgerId = draft.ledgerId,
                currencyKey = snapshot.currencyKey,
                baseAmountMinor = snapshot.baseAmountMinor,
                baseCurrencyKey = snapshot.baseCurrencyKey,
            ),
        )
        if (updated == 0) throw AccountingWriteException("账目不存在或已被删除")
    }

    /**
     * 删除指定账目。
     */
    suspend fun deleteTransaction(transactionId: Long) {
        if (dao.deleteTransaction(transactionId) == 0) {
            throw AccountingWriteException("账目不存在或已被删除")
        }
    }

    /**
     * 新增或更新一个账户。
     */
    suspend fun saveAccount(account: AccountEntity, ledgerIds: Set<Long>): Long {
        val accountType = dao.findAccountType(account.typeKey)
            ?: throw AccountingWriteException("请选择账户类型")
        if (dao.findCurrency(account.currencyKey) == null) {
            throw AccountingWriteException("请选择账户币种")
        }
        val normalized = account.copy(
            name = account.name.trim(),
            type = accountType.baseType,
            sortOrder = if (account.id == 0L) dao.maxAccountSortOrder() + 1 else account.sortOrder,
            isDefault = account.isDefault && !account.isArchived,
        )
        if (normalized.name.isEmpty()) throw AccountingWriteException("账户名称不能为空")
        if (normalized.iconKey.isBlank()) throw AccountingWriteException("请选择账户图标")
        if (kotlin.math.abs(normalized.openingBalanceMinor) > MAX_AMOUNT_MINOR) {
            throw AccountingWriteException("期初余额超出上限")
        }
        if (ledgerIds.isEmpty()) throw AccountingWriteException("请至少选择一个适用账本")
        val existing = if (account.id == 0L) null else dao.findAccount(account.id)
            ?: throw AccountingWriteException("账户不存在或已被删除")
        if (existing != null && existing.currencyKey != account.currencyKey && !isAccountEmpty(existing)) {
            return dao.exchangeAccountCurrency(existing, normalized.copy(isArchived = false), ledgerIds)
        }
        return dao.saveAccountWithLedgers(normalized, ledgerIds)
    }

    /** 判断账户是否为空，空账户可直接修改币种而不重释历史金额。 */
    private suspend fun isAccountEmpty(account: AccountEntity): Boolean =
        account.openingBalanceMinor == 0L && dao.countTransactionsByAccountId(account.id) == 0

    /** 新增账本并返回其标识。 */
    suspend fun addLedger(
        name: String,
        coverKey: String,
        useLightText: Boolean,
        baseCurrencyKey: String,
        isHidden: Boolean,
    ): Long {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("账本名称不能为空")
        if (dao.findCurrency(baseCurrencyKey) == null) throw AccountingWriteException("请选择本位币")
        val id = dao.insertLedger(
            LedgerEntity(
                name = normalizedName,
                coverKey = coverKey,
                useLightText = useLightText,
                baseCurrencyKey = baseCurrencyKey,
                isHidden = isHidden,
                sortOrder = dao.maxLedgerSortOrder() + 1,
            ),
        )
        if (id == -1L) throw AccountingWriteException("账本名称不能重复")
        return id
    }

    /** 更新账本名称、封面、本位币和隐藏状态。 */
    suspend fun updateLedger(ledger: LedgerEntity) {
        if (ledger.name.trim().isEmpty()) throw AccountingWriteException("账本名称不能为空")
        if (dao.findCurrency(ledger.baseCurrencyKey) == null) throw AccountingWriteException("请选择本位币")
        when (dao.updateLedgerSafely(ledger.copy(name = ledger.name.trim()))) {
            LedgerMutationResult.SUCCESS -> Unit
            LedgerMutationResult.CURRENT_LEDGER ->
                throw AccountingWriteException("当前账本不能隐藏，请先切换到其他账本")
            LedgerMutationResult.NOT_FOUND -> throw AccountingWriteException("账本不存在")
            LedgerMutationResult.SETTINGS_MISSING -> throw AccountingWriteException("当前账本设置不存在，请重试")
            else -> throw AccountingWriteException("账本更新失败")
        }
    }

    /** 切换三个主分页共同使用的当前账本。 */
    suspend fun selectLedger(ledgerId: Long) {
        when (dao.selectLedgerSafely(ledgerId)) {
            LedgerMutationResult.SUCCESS -> Unit
            LedgerMutationResult.NOT_FOUND -> throw AccountingWriteException("账本不存在")
            LedgerMutationResult.HIDDEN_LEDGER -> throw AccountingWriteException("请先取消隐藏该账本")
            LedgerMutationResult.SETTINGS_MISSING -> throw AccountingWriteException("当前账本设置不存在，请重试")
            else -> throw AccountingWriteException("账本切换失败")
        }
    }

    /** 删除无明细且无账户关联的非当前账本。 */
    suspend fun deleteLedger(ledgerId: Long) {
        when (dao.deleteLedgerSafely(ledgerId)) {
            LedgerMutationResult.SUCCESS -> Unit
            LedgerMutationResult.NOT_FOUND -> throw AccountingWriteException("账本不存在")
            LedgerMutationResult.CURRENT_LEDGER ->
                throw AccountingWriteException("当前账本不能删除，请先切换到其他账本")
            LedgerMutationResult.DEFAULT_LEDGER -> throw AccountingWriteException("默认账本不能删除")
            LedgerMutationResult.HAS_TRANSACTIONS ->
                throw AccountingWriteException("账本中已有明细，不能删除")
            LedgerMutationResult.HAS_ACCOUNT_LINKS ->
                throw AccountingWriteException("账本仍有关联账户，请先移除关联后再删除")
            LedgerMutationResult.SETTINGS_MISSING -> throw AccountingWriteException("当前账本设置不存在，请重试")
            else -> throw AccountingWriteException("账本删除失败")
        }
    }

    /**
     * 新增名称、符号与汇率均由用户指定的自定义币种。
     */
    suspend fun addCurrency(
        name: String,
        symbol: String,
        rateToCnyScaled: Long,
    ): String {
        val normalizedName = name.trim()
        val normalizedSymbol = symbol.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("币种名称不能为空")
        if (normalizedSymbol.isEmpty()) throw AccountingWriteException("币种符号不能为空")
        if (rateToCnyScaled <= 0) throw AccountingWriteException("币种汇率必须大于零")
        if (rateToCnyScaled > MAX_RATE_TO_CNY_SCALED) throw AccountingWriteException("币种汇率超出上限")
        val currencyKey = "custom_${System.currentTimeMillis()}"
        val inserted = dao.insertCurrency(
            CurrencyEntity(
                key = currencyKey,
                code = "",
                name = normalizedName,
                symbol = normalizedSymbol,
                rateToCnyScaled = rateToCnyScaled,
                isBuiltin = false,
                updatedAt = System.currentTimeMillis(),
                autoRateEnabled = false,
            ),
        )
        if (inserted == -1L) throw AccountingWriteException("币种名称不能重复")
        return currencyKey
    }

    /** 编辑自定义币种，或在关闭自动汇率后编辑预置币种的手动汇率。 */
    suspend fun updateCurrency(
        currencyKey: String,
        name: String,
        symbol: String,
        rateToCnyScaled: Long,
    ) {
        val currency = dao.findCurrency(currencyKey)
            ?: throw AccountingWriteException("币种不存在")
        if (currency.code == "CNY") throw AccountingWriteException("人民币是本位币，汇率固定为一")
        if (currency.isBuiltin && currency.autoRateEnabled) {
            throw AccountingWriteException("请先关闭自动汇率")
        }
        val normalizedName = if (currency.isBuiltin) currency.name else name.trim()
        val normalizedSymbol = if (currency.isBuiltin) currency.symbol else symbol.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("币种名称不能为空")
        if (normalizedSymbol.isEmpty()) throw AccountingWriteException("币种符号不能为空")
        if (rateToCnyScaled <= 0) throw AccountingWriteException("币种汇率必须大于零")
        if (rateToCnyScaled > MAX_RATE_TO_CNY_SCALED) throw AccountingWriteException("币种汇率超出上限")
        if (dao.countOtherCurrenciesByName(currencyKey, normalizedName) > 0) {
            throw AccountingWriteException("币种名称不能重复")
        }
        if (dao.updateCurrency(
                currencyKey,
                normalizedName,
                normalizedSymbol,
                rateToCnyScaled,
                System.currentTimeMillis(),
            ) == 0
        ) {
            throw AccountingWriteException("币种更新失败")
        }
    }

    /** 删除未被账户或账本使用的自定义币种。 */
    suspend fun deleteCurrency(currencyKey: String) {
        when (dao.deleteCustomCurrencySafely(currencyKey)) {
            CurrencyDeleteResult.SUCCESS -> Unit
            CurrencyDeleteResult.NOT_FOUND -> throw AccountingWriteException("币种不存在")
            CurrencyDeleteResult.BUILTIN_CURRENCY -> throw AccountingWriteException("预置币种不能删除")
            CurrencyDeleteResult.ACCOUNT_LINKS -> throw AccountingWriteException("该币种正在被账户使用")
            CurrencyDeleteResult.LEDGER_LINKS -> throw AccountingWriteException("该币种正在被账本作为本位币使用")
            CurrencyDeleteResult.TRANSACTION_LINKS ->
                throw AccountingWriteException("该币种仍被历史账目使用，无法删除")
            CurrencyDeleteResult.DELETE_FAILED -> throw AccountingWriteException("币种删除失败")
        }
    }

    /** 设置预置币种的自动汇率状态，开启时立即尝试刷新当前币种。 */
    suspend fun setCurrencyAutoRate(currencyKey: String, enabled: Boolean) {
        val currency = dao.findCurrency(currencyKey)
            ?: throw AccountingWriteException("币种不存在")
        if (!currency.isBuiltin) throw AccountingWriteException("自定义币种使用手动汇率")
        if (currency.code == "CNY") throw AccountingWriteException("人民币是本位币，汇率固定为一")
        if (dao.updateCurrencyAutoRate(currencyKey, enabled) == 0) {
            throw AccountingWriteException("自动汇率设置失败")
        }
        if (enabled) {
            val rate = withContext(Dispatchers.IO) {
                fetchCurrencyRates(setOf(currency.code))[currency.code]
            }
            if (rate != null) {
                dao.updateBuiltinCurrencyRate(currency.code, rate, System.currentTimeMillis())
            }
        }
    }

    /**
     * 新增一个可复用的自定义账户类型。
     */
    suspend fun addAccountType(
        name: String,
        summary: String,
    ): String {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("账户类型名称不能为空")
        val typeKey = "custom_${System.currentTimeMillis()}"
        val inserted = dao.insertAccountType(
            AccountTypeEntity(
                key = typeKey,
                name = normalizedName,
                summary = summary.trim(),
                iconKey = "virtual_account",
                baseType = AccountType.VIRTUAL,
            ),
        )
        if (inserted == -1L) throw AccountingWriteException("账户类型名称不能重复")
        return typeKey
    }

    /**
     * 删除未被账户引用的自定义账户类型。
     */
    suspend fun deleteAccountType(typeKey: String) {
        val accountType = dao.findAccountType(typeKey)
            ?: throw AccountingWriteException("账户类型不存在")
        if (accountType.isBuiltin) throw AccountingWriteException("预置账户类型不能删除")
        if (dao.countAccountsByTypeKey(typeKey) > 0) {
            throw AccountingWriteException("该账户类型正在被账户使用")
        }
        if (dao.deleteCustomAccountType(typeKey) == 0) {
            throw AccountingWriteException("账户类型删除失败")
        }
    }

    /**
     * 更新一个自定义账户类型的名称与可选说明。
     */
    suspend fun updateAccountType(
        typeKey: String,
        name: String,
        summary: String,
    ) {
        val accountType = dao.findAccountType(typeKey)
            ?: throw AccountingWriteException("账户类型不存在")
        if (accountType.isBuiltin) throw AccountingWriteException("预置账户类型不能编辑")
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("账户类型名称不能为空")
        if (dao.countOtherAccountTypesByName(typeKey, normalizedName) > 0) {
            throw AccountingWriteException("账户类型名称不能重复")
        }
        if (dao.updateCustomAccountType(typeKey, normalizedName, summary.trim()) == 0) {
            throw AccountingWriteException("账户类型更新失败")
        }
    }

    /**
     * 停用指定账户。
     */
    suspend fun archiveAccount(accountId: Long) {
        if (dao.findAccount(accountId) == null) {
            throw AccountingWriteException("账户不存在或已被删除")
        }
        dao.archiveAccount(accountId)
    }

    /** 删除没有历史账目的账户。 */
    suspend fun deleteAccount(accountId: Long) {
        if (dao.findAccount(accountId) == null) {
            throw AccountingWriteException("账户不存在或已被删除")
        }
        if (dao.countTransactionsByAccountId(accountId) > 0) {
            throw AccountingWriteException("账户已有历史账目，无法删除")
        }
        if (dao.deleteAccount(accountId) == 0) {
            throw AccountingWriteException("账户删除失败")
        }
    }

    /**
     * 新增指定收支方向的分类。
     */
    suspend fun addCategory(
        name: String,
        type: TransactionType,
        iconKey: String,
    ): Long {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("分类名称不能为空")
        if (iconKey.isBlank()) throw AccountingWriteException("请选择分类图标")
        val categoryId = dao.insertCategory(
            CategoryEntity(
                name = normalizedName,
                type = type,
                sortOrder = dao.maxCategorySortOrder(type) + 1,
                iconKey = iconKey,
            ),
        )
        if (categoryId == -1L) {
            throw AccountingWriteException("同方向分类名称不能重复")
        }
        return categoryId
    }

    /**
     * 更新分类名称与图标，方向、排序与归档状态保持不变。
     */
    suspend fun updateCategory(categoryId: Long, name: String, iconKey: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) throw AccountingWriteException("分类名称不能为空")
        if (iconKey.isBlank()) throw AccountingWriteException("请选择分类图标")
        val category = dao.findCategory(categoryId)
            ?: throw AccountingWriteException("分类不存在")
        if (dao.countOtherCategoriesByName(categoryId, normalizedName, category.type) > 0) {
            throw AccountingWriteException("同方向分类名称不能重复")
        }
        dao.updateCategory(category.copy(name = normalizedName, iconKey = iconKey))
    }

    /** 停用指定分类，历史账目继续保留引用。 */
    suspend fun archiveCategory(categoryId: Long) {
        if (dao.findCategory(categoryId) == null) throw AccountingWriteException("分类不存在")
        dao.archiveCategory(categoryId)
    }

    /** 根据既有精确映射和可用账本数据预填自动账单。 */
    private suspend fun predictAutoBookkeeping(
        capture: AutoBookkeepingCapture,
        ledgerId: Long,
        merchantKey: String,
        paymentMethodKey: String,
    ): AutoPrediction {
        val categoryMapping = merchantKey.takeIf(String::isNotEmpty)?.let {
            dao.findAutoCategoryMapping(capture.provider, it, capture.type)
        }
        val categories = dao.getAllCategories().filter { !it.isArchived && it.type == capture.type }
        val categoryId = categoryMapping?.categoryId
            ?: inferAutoCategoryName(capture.merchant)?.let { name ->
                categories.firstOrNull { it.name == name }?.id
            }
        val refs = dao.getAllAccountLedgerCrossRefs().filter { it.ledgerId == ledgerId }
        val accounts = dao.getAllAccounts().filter { account ->
            !account.isArchived && refs.any { it.accountId == account.id }
        }
        val sourceMapping = paymentMethodKey.takeIf(String::isNotEmpty)?.let {
            dao.findAutoAccountMapping(capture.provider, it)
        }
        val mappedAccount = sourceMapping?.accountId?.takeIf { id ->
            accounts.any { it.id == id && it.currencyKey == capture.currencyKey }
        }
        val accountId = mappedAccount ?: accounts.singleOrNull { it.currencyKey == capture.currencyKey }?.id
        return AutoPrediction(
            accountId = accountId,
            categoryId = categoryId,
            canConfirm = mappedAccount != null && categoryMapping != null,
        )
    }

    /** 表示本地匹配为待确认账单提供的预填字段与置信度。 */
    private data class AutoPrediction(
        val accountId: Long?,
        val categoryId: Long?,
        val canConfirm: Boolean,
    )

    /** 承载一次用户确认需要与正式流水原子保存的学习映射。 */
    private data class AutoMappings(
        val category: AutoCategoryMappingEntity? = null,
        val accounts: List<AutoAccountMappingEntity> = emptyList(),
    )

    /** 把已经补全的自动账单事件转换为统一交易草稿。 */
    private fun AutoBookkeepingEventEntity.toTransactionDraft(): TransactionDraft = TransactionDraft(
        type = type,
        amountMinor = amountMinor,
        currencyKey = currencyKey,
        accountAmountMinor = amountMinor,
        accountId = requireNotNull(accountId),
        categoryId = categoryId,
        merchant = merchant,
        note = note,
        occurredAt = occurredAt,
        source = TransactionSource.AI,
        ledgerId = ledgerId,
    )

    /** 把一个已校验草稿转换成单条收支账目。 */
    private suspend fun transactionEntities(draft: TransactionDraft): List<TransactionEntity> {
        val snapshot = transactionSnapshot(draft)
        return listOf(
            TransactionEntity(
                type = draft.type,
                amountMinor = draft.amountMinor,
                accountAmountMinor = snapshot.accountAmountMinor,
                accountId = draft.accountId,
                categoryId = draft.categoryId,
                merchant = draft.merchant.trim(),
                note = draft.note.trim(),
                occurredAt = draft.occurredAt,
                source = draft.source,
                ledgerId = draft.ledgerId,
                currencyKey = snapshot.currencyKey,
                baseAmountMinor = snapshot.baseAmountMinor,
                baseCurrencyKey = snapshot.baseCurrencyKey,
            ),
        )
    }

    /** 按写入时的账户币种与账本位币固化币种与本位币金额快照。 */
    private suspend fun transactionSnapshot(draft: TransactionDraft): TransactionSnapshot {
        val ledgerId = draft.ledgerId
        val account = dao.findAccount(draft.accountId)
            ?: throw AccountingWriteException("所选账户不存在")
        val ledger = dao.findLedger(ledgerId)
            ?: throw AccountingWriteException("所选账本不存在")
        val transactionCurrency = dao.findCurrency(draft.currencyKey.ifBlank { account.currencyKey })
            ?: throw AccountingWriteException("交易币种不存在")
        val accountCurrency = dao.findCurrency(account.currencyKey)
            ?: throw AccountingWriteException("账户币种不存在")
        val baseCurrency = dao.findCurrency(ledger.baseCurrencyKey)
            ?: throw AccountingWriteException("账本位币不存在")
        val accountAmountMinor = draft.accountAmountMinor.takeIf { it > 0 } ?: draft.amountMinor
        val baseAmountMinor = try {
            convertCurrencyMinor(
                amountMinor = accountAmountMinor,
                sourceRateToCnyScaled = accountCurrency.rateToCnyScaled,
                targetRateToCnyScaled = baseCurrency.rateToCnyScaled,
            )
        } catch (_: ArithmeticException) {
            throw AccountingWriteException("金额超出可计算范围")
        }
        return TransactionSnapshot(
            currencyKey = transactionCurrency.key,
            accountAmountMinor = accountAmountMinor,
            baseAmountMinor = baseAmountMinor,
            baseCurrencyKey = baseCurrency.key,
        )
    }

    /** 表示账目写入时固化的币种与本位币金额快照。 */
    private data class TransactionSnapshot(
        val currencyKey: String,
        val accountAmountMinor: Long,
        val baseAmountMinor: Long,
        val baseCurrencyKey: String,
    )

    /** 按账目历史本位币与账本当前本位币换算展示金额，币种未改变时保留原快照。 */
    private fun convertBaseAmounts(
        records: List<TransactionRecord>,
        ledgers: List<LedgerRecord>,
        currencies: List<CurrencyEntity>,
    ): List<TransactionRecord> {
        val ledgerCurrencies = ledgers.associate { it.id to it.baseCurrencyKey }
        val rates = currencies.associate { it.key to it.rateToCnyScaled }
        return records.map { record ->
            val currentBaseCurrencyKey = ledgerCurrencies.getValue(record.ledgerId)
            if (record.baseCurrencyKey == currentBaseCurrencyKey) record else record.copy(
                baseAmountMinor = convertCurrencyMinor(
                    record.baseAmountMinor,
                    rates.getValue(record.baseCurrencyKey),
                    rates.getValue(currentBaseCurrencyKey),
                ),
            )
        }
    }

    private suspend fun validateTransactionDraft(
        draft: TransactionDraft,
        original: TransactionEntity? = null,
    ) {
        if (draft.amountMinor <= 0) throw AccountingWriteException("金额必须大于零")
        if (draft.amountMinor > MAX_AMOUNT_MINOR) throw AccountingWriteException("金额超出上限")
        if (draft.accountAmountMinor < 0 || draft.accountAmountMinor > MAX_AMOUNT_MINOR) {
            throw AccountingWriteException("账户金额超出上限")
        }
        if (draft.occurredAt <= 0) throw AccountingWriteException("记账时间无效")
        val ledgerId = draft.ledgerId
        if (dao.findLedger(ledgerId) == null) {
            throw AccountingWriteException("所选账本不存在")
        }
        val account = dao.findAccount(draft.accountId)
            ?: throw AccountingWriteException("所选账户不存在")
        if (account.isArchived && original?.accountId != account.id) {
            throw AccountingWriteException("所选账户已停用")
        }
        val keepsOriginalLink = original != null &&
            original.accountId == account.id &&
            original.ledgerId == ledgerId
        if (!keepsOriginalLink && dao.findAccountLedgerCrossRef(account.id, ledgerId) == null) {
            throw AccountingWriteException("所选账户不属于当前账本")
        }
        val categoryId = draft.categoryId ?: throw AccountingWriteException("请选择分类")
        val category = dao.findCategory(categoryId)
            ?: throw AccountingWriteException("所选分类不存在")
        if (category.isArchived && original?.categoryId != category.id) {
            throw AccountingWriteException("所选分类已停用")
        }
        if (category.type != draft.type) {
            throw AccountingWriteException("分类与收支类型不一致")
        }
    }
}

private const val AUTO_EVENT_RETENTION_MILLIS = 90L * 24 * 60 * 60 * 1000
private const val AUTO_DUPLICATE_WINDOW_MILLIS = 3L * 60 * 1000

/** 判断应用设置是否允许采集指定支付平台。 */
private fun AppSettingsEntity.isProviderEnabled(provider: PaymentProvider): Boolean = when (provider) {
    PaymentProvider.WECHAT -> autoBookkeepingWechatEnabled
    PaymentProvider.ALIPAY -> autoBookkeepingAlipayEnabled
    PaymentProvider.UNIONPAY -> autoBookkeepingUnionPayEnabled
}

/** 把商户或支付方式转换为稳定、无空白差异的本地映射键。 */
private fun normalizeAutoKey(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("\\s+"), "")

/** 为缺少外部交易号的采集结果生成不含原始页面文本的防重摘要。 */
private fun autoFingerprint(
    capture: AutoBookkeepingCapture,
    merchantKey: String,
    paymentMethodKey: String,
): String = sha256(
    listOf(
        capture.provider.name,
        capture.type.name,
        capture.amountMinor.toString(),
        merchantKey,
        paymentMethodKey,
    ).joinToString("|"),
)

/** 使用 SHA-256 生成仅供本地比较的十六进制摘要。 */
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }

/** 根据有限关键词返回分类预填名称，结果不直接取得高置信度。 */
private fun inferAutoCategoryName(merchant: String): String? {
    val value = merchant.lowercase()
    return when {
        listOf("餐", "咖啡", "奶茶", "美团", "饿了么").any(value::contains) -> "餐饮"
        listOf("滴滴", "铁路", "公交", "地铁", "加油", "停车").any(value::contains) -> "交通"
        listOf("淘宝", "京东", "拼多多", "商场", "超市").any(value::contains) -> "购物"
        listOf("电影", "游戏", "视频", "音乐").any(value::contains) -> "娱乐"
        listOf("医院", "药房", "诊所").any(value::contains) -> "医疗"
        listOf("话费", "流量", "宽带").any(value::contains) -> "通讯"
        listOf("房租", "物业", "水费", "电费", "燃气").any(value::contains) -> "住房"
        else -> null
    }
}
