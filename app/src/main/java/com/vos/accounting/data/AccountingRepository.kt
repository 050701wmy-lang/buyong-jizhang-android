package com.vos.accounting.data

import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * 表示用户可修正的账务写入错误。
 */
class AccountingWriteException(message: String) : IllegalArgumentException(message)

/**
 * 汇总账本数据读取与统一写入流程。
 */
class AccountingRepository(
    private val dao: AccountingDao,
    private val currencyRateService: CurrencyRateService = CurrencyRateService(),
) {
    val accounts = dao.observeAccounts()
    val ledgers = dao.observeLedgers()
    val accountLedgerCrossRefs = dao.observeAccountLedgerCrossRefs()
    val accountTypes = dao.observeAccountTypes()
    val currencies = dao.observeCurrencies()
    val categories = dao.observeCategories()
    val transactions = dao.observeTransactions()
    val allTransactions = dao.observeAllTransactions()
    val overviewTotals = dao.observeOverviewTotals()
    val expenseCategoryTotals = dao.observeExpenseCategoryTotals()
    val settings = dao.observeSettings()

    /**
     * 建立首次启动所需的默认账本数据。
     */
    suspend fun initialize() {
        dao.seedDefaults()
    }

    /**
     * 联网刷新预置币种兑人民币汇率，失败时保留本地值。
     */
    suspend fun refreshBuiltinCurrencyRates() {
        val builtinCurrencies = currencies.first().filter {
            it.isBuiltin && it.autoRateEnabled
        }
        val rates = withContext(Dispatchers.IO) {
            currencyRateService.fetchRates(builtinCurrencies.map(CurrencyEntity::code).toSet())
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

    /**
     * 把已经确认的草稿写入正式账目。
     */
    suspend fun saveTransaction(draft: TransactionDraft): Long {
        validateTransactionDraft(draft)
        val snapshot = transactionSnapshot(draft)
        return dao.insertTransaction(
            TransactionEntity(
                type = draft.type,
                amountMinor = draft.amountMinor,
                accountId = draft.accountId,
                categoryId = draft.categoryId,
                merchant = draft.merchant.trim(),
                note = draft.note.trim(),
                occurredAt = draft.occurredAt,
                source = draft.source,
                ledgerId = draft.ledgerId,
                currencyKey = snapshot.currencyKey,
                baseAmountMinor = snapshot.baseAmountMinor,
            ),
        )
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
        val snapshot = transactionSnapshot(draft, original)
        val updated = dao.updateTransaction(
            TransactionEntity(
                id = transactionId,
                type = draft.type,
                amountMinor = draft.amountMinor,
                accountId = draft.accountId,
                categoryId = draft.categoryId,
                merchant = draft.merchant.trim(),
                note = draft.note.trim(),
                occurredAt = draft.occurredAt,
                source = draft.source,
                ledgerId = original.ledgerId,
                currencyKey = snapshot.currencyKey,
                baseAmountMinor = snapshot.baseAmountMinor,
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
        if (ledgerIds.isEmpty()) throw AccountingWriteException("请至少选择一个适用账本")
        return dao.saveAccountWithLedgers(normalized, ledgerIds)
    }

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
                currencyRateService.fetchRates(setOf(currency.code))[currency.code]
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
     * 校验草稿引用的数据与收支方向，并允许编辑账目继续引用原有停用项。
     */
    /**
     * 按写入时的账户币种与账本位币固化币种与本位币金额快照。
     */
    private suspend fun transactionSnapshot(
        draft: TransactionDraft,
        original: TransactionEntity? = null,
    ): TransactionSnapshot {
        val ledgerId = original?.ledgerId ?: draft.ledgerId
        val account = dao.findAccount(draft.accountId)
            ?: throw AccountingWriteException("所选账户不存在")
        val ledger = dao.findLedger(ledgerId)
            ?: throw AccountingWriteException("所选账本不存在")
        val currency = dao.findCurrency(account.currencyKey)
            ?: throw AccountingWriteException("账户币种不存在")
        val baseCurrency = dao.findCurrency(ledger.baseCurrencyKey)
            ?: throw AccountingWriteException("账本位币不存在")
        return TransactionSnapshot(
            currencyKey = currency.key,
            baseAmountMinor = convertCurrencyMinor(
                amountMinor = draft.amountMinor,
                sourceRateToCnyScaled = currency.rateToCnyScaled,
                targetRateToCnyScaled = baseCurrency.rateToCnyScaled,
            ),
        )
    }

    /** 表示账目写入时固化的币种与本位币金额快照。 */
    private data class TransactionSnapshot(
        val currencyKey: String,
        val baseAmountMinor: Long,
    )

    private suspend fun validateTransactionDraft(
        draft: TransactionDraft,
        original: TransactionEntity? = null,
    ) {
        if (draft.amountMinor <= 0) throw AccountingWriteException("金额必须大于零")
        if (draft.occurredAt <= 0) throw AccountingWriteException("记账时间无效")
        val ledgerId = original?.ledgerId ?: draft.ledgerId
        if (dao.findLedger(ledgerId) == null) {
            throw AccountingWriteException("所选账本不存在")
        }
        val account = dao.findAccount(draft.accountId)
            ?: throw AccountingWriteException("所选账户不存在")
        if (account.isArchived && original?.accountId != account.id) {
            throw AccountingWriteException("所选账户已停用")
        }
        val category = dao.findCategory(draft.categoryId)
            ?: throw AccountingWriteException("所选分类不存在")
        if (category.isArchived && original?.categoryId != category.id) {
            throw AccountingWriteException("所选分类已停用")
        }
        if (category.type != draft.type) {
            throw AccountingWriteException("分类与收支类型不一致")
        }
        val keepsOriginalLink = original != null &&
            original.accountId == account.id &&
            original.ledgerId == ledgerId
        if (!keepsOriginalLink && dao.findAccountLedgerCrossRef(account.id, ledgerId) == null) {
            throw AccountingWriteException("所选账户不属于当前账本")
        }
    }
}
