package com.vos.accounting.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vos.accounting.backup.BackupManager
import com.vos.accounting.backup.PreparedBackup
import com.vos.accounting.data.AccountBalanceRecord
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountExchangeLink
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.AutoBookkeepingEventEntity
import com.vos.accounting.data.AutoHookHeartbeatEntity
import com.vos.accounting.data.AutoRulePackEntity
import com.vos.accounting.auto.AutoAiCredentialStore
import com.vos.accounting.auto.AutoBookkeepingRuleEngine
import com.vos.accounting.auto.PrivateAutoBookkeepingAiClient
import com.vos.accounting.auto.RulePackV1
import com.vos.accounting.auto.testRootScreenshotAccess
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.LedgerEntity
import com.vos.accounting.data.LedgerRecord
import com.vos.accounting.data.AccountLedgerCrossRef
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.NotificationPrivacyMode
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 表示一次账务写操作的进行状态与可展示错误。
 */
data class AccountingWriteState(
    val inProgress: Boolean = false,
    val error: String? = null,
)

/**
 * 汇总所有主页面需要的账本状态。
 */
data class AccountingUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val accountTypes: List<AccountTypeEntity> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val ledgers: List<LedgerRecord> = emptyList(),
    val accountLedgerCrossRefs: List<AccountLedgerCrossRef> = emptyList(),
    val currentLedgerId: Long = 1,
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val accountBalances: Map<Long, AccountBalanceRecord> = emptyMap(),
    val accountExchangeLinks: List<AccountExchangeLink> = emptyList(),
    val writeInProgress: Boolean = false,
    val writeError: String? = null,
    val themeMode: AccountingThemeMode = AccountingThemeMode.SYSTEM,
    val followSystemColor: Boolean = true,
    val predictiveBackAnimationEnabled: Boolean = false,
    val coloredTransactionAmountsEnabled: Boolean = false,
    val autoBookkeepingEnabled: Boolean = false,
    val autoBookkeepingWechatEnabled: Boolean = true,
    val autoBookkeepingAlipayEnabled: Boolean = true,
    val autoBookkeepingUnionPayEnabled: Boolean = true,
    val notificationPrivacyMode: NotificationPrivacyMode = NotificationPrivacyMode.HIDE_ON_LOCK_SCREEN,
    val xiaomiSuperIslandEnabled: Boolean = true,
    val autoLocalOcrEnabled: Boolean = false,
    val autoRootOcrEnabled: Boolean = false,
    val autoXposedEnabled: Boolean = false,
    val autoCloudAiEnabled: Boolean = false,
    val autoAiBaseUrl: String = "",
    val autoAiModel: String = "",
    val autoAiVisionEnabled: Boolean = false,
    val autoAiAllowInsecureLanHttp: Boolean = false,
    val autoAiAllowOneTapConfirm: Boolean = false,
    val autoHookHeartbeats: List<AutoHookHeartbeatEntity> = emptyList(),
    val pendingAutoBookkeepingEvents: List<AutoBookkeepingEventEntity> = emptyList(),
)

/** 汇集账户、类型、币种及账本关联的响应式状态。 */
private data class LedgerAccountState(
    val accounts: List<AccountEntity>,
    val accountTypes: List<AccountTypeEntity>,
    val currencies: List<CurrencyEntity>,
    val ledgers: List<LedgerRecord>,
    val refs: List<AccountLedgerCrossRef>,
)

/**
 * 管理账本界面状态和统一保存动作。
 */
class AccountingViewModel(
    private val repository: AccountingRepository,
    private val backupManager: BackupManager,
) : ViewModel() {
    private val writeState = MutableStateFlow(AccountingWriteState())
    private var pendingLedgerId: Long? = null
    private var pendingLedgerSelection: (() -> Unit)? = null
    private var ledgerSelectionJob: Job? = null
    val autoRulePacks = repository.autoRulePacks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val accountState = combine(
        repository.accounts,
        repository.accountTypes,
        repository.currencies,
        repository.ledgers,
        repository.accountLedgerCrossRefs,
    ) { accounts, accountTypes, currencies, ledgers, refs ->
        LedgerAccountState(accounts, accountTypes, currencies, ledgers, refs)
    }

    private val ledgerState = combine(
        accountState,
        repository.categories,
        repository.transactions,
    ) { accountState, categories, transactions ->
        AccountingUiState(
            accounts = accountState.accounts,
            accountTypes = accountState.accountTypes,
            currencies = accountState.currencies,
            ledgers = accountState.ledgers,
            accountLedgerCrossRefs = accountState.refs,
            categories = categories,
            transactions = transactions,
        )
    }

    private val ledgerStateWithAccountFacts = combine(
        ledgerState,
        repository.accountBalances,
        repository.accountExchangeLinks,
    ) { state, accountBalances, accountExchangeLinks ->
        state.copy(
            accountBalances = accountBalances,
            accountExchangeLinks = accountExchangeLinks,
        )
    }

    val uiState = combine(
        ledgerStateWithAccountFacts,
        repository.settings,
        writeState,
        repository.pendingAutoBookkeepingEvents,
        repository.autoHookHeartbeats,
    ) { ledger, settings, write, pendingEvents, hookHeartbeats ->
        ledger.copy(
            writeInProgress = write.inProgress,
            writeError = write.error,
            themeMode = settings?.themeMode?.let { mode ->
                AccountingThemeMode.entries.firstOrNull { it.name == mode }
            } ?: AccountingThemeMode.SYSTEM,
            followSystemColor = settings?.followSystemColor ?: true,
            predictiveBackAnimationEnabled = settings?.predictiveBackAnimationEnabled ?: false,
            coloredTransactionAmountsEnabled = settings?.coloredTransactionAmountsEnabled ?: false,
            autoBookkeepingEnabled = settings?.autoBookkeepingEnabled ?: false,
            autoBookkeepingWechatEnabled = settings?.autoBookkeepingWechatEnabled ?: true,
            autoBookkeepingAlipayEnabled = settings?.autoBookkeepingAlipayEnabled ?: true,
            autoBookkeepingUnionPayEnabled = settings?.autoBookkeepingUnionPayEnabled ?: true,
            notificationPrivacyMode = settings?.notificationPrivacyMode
                ?: NotificationPrivacyMode.HIDE_ON_LOCK_SCREEN,
            xiaomiSuperIslandEnabled = settings?.xiaomiSuperIslandEnabled ?: true,
            autoLocalOcrEnabled = settings?.autoLocalOcrEnabled ?: false,
            autoRootOcrEnabled = settings?.autoRootOcrEnabled ?: false,
            autoXposedEnabled = settings?.autoXposedEnabled ?: false,
            autoCloudAiEnabled = settings?.autoCloudAiEnabled ?: false,
            autoAiBaseUrl = settings?.autoAiBaseUrl.orEmpty(),
            autoAiModel = settings?.autoAiModel.orEmpty(),
            autoAiVisionEnabled = settings?.autoAiVisionEnabled ?: false,
            autoAiAllowInsecureLanHttp = settings?.autoAiAllowInsecureLanHttp ?: false,
            autoAiAllowOneTapConfirm = settings?.autoAiAllowOneTapConfirm ?: false,
            autoHookHeartbeats = hookHeartbeats,
            pendingAutoBookkeepingEvents = pendingEvents,
            currentLedgerId = settings?.currentLedgerId ?: 1,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountingUiState(),
    )

    init {
        viewModelScope.launch {
            repository.initialize()
            repository.refreshBuiltinCurrencyRates()
        }
    }

    /**
     * 清除已经向用户展示的账务写入错误。
     */
    fun clearWriteError() {
        writeState.value = writeState.value.copy(error = null)
    }

    /**
     * 更新应用外观的明暗模式。
     */
    fun updateThemeMode(themeMode: AccountingThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode.name)
        }
    }

    /**
     * 更新应用是否跟随系统配色。
     */
    fun updateFollowSystemColor(followSystemColor: Boolean) {
        viewModelScope.launch {
            repository.updateFollowSystemColor(followSystemColor)
        }
    }

    /**
     * 更新应用是否启用预测性返回动画。
     */
    fun updatePredictiveBackAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePredictiveBackAnimationEnabled(enabled)
        }
    }

    /** 更新普通收支金额是否使用红绿字体。 */
    fun updateColoredTransactionAmountsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateColoredTransactionAmountsEnabled(enabled)
        }
    }

    /** 更新自动记账总开关。 */
    fun updateAutoBookkeepingEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoBookkeepingEnabled(enabled) }
    }

    /** 更新指定支付平台的自动记账开关。 */
    fun updateAutoBookkeepingProviderEnabled(provider: PaymentProvider, enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoBookkeepingProviderEnabled(provider, enabled) }
    }

    /** 更新自动账单通知的隐私展示方式。 */
    fun updateNotificationPrivacyMode(mode: NotificationPrivacyMode) {
        viewModelScope.launch { repository.updateNotificationPrivacyMode(mode) }
    }

    /** 更新小米超级岛通知样式开关。 */
    fun updateXiaomiSuperIslandEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateXiaomiSuperIslandEnabled(enabled) }
    }

    /** 更新本地 OCR 开关。 */
    fun updateAutoLocalOcrEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoLocalOcrEnabled(enabled) }
    }

    /** 更新 Root OCR 开关。 */
    fun updateAutoRootOcrEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoRootOcrEnabled(enabled) }
    }

    /** 更新 LSPosed 采集开关。 */
    fun updateAutoXposedEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoXposedEnabled(enabled) }
    }

    /** 更新私有云 AI 开关。 */
    fun updateAutoCloudAiEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoCloudAiEnabled(enabled) }
    }

    /** 更新视觉识别风险开关。 */
    fun updateAutoAiVisionEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoAiVisionEnabled(enabled) }
    }

    /** 更新私网 HTTP 风险开关。 */
    fun updateAutoAiAllowInsecureLanHttp(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoAiAllowInsecureLanHttp(enabled) }
    }

    /** 更新 AI 一键确认风险开关。 */
    fun updateAutoAiAllowOneTapConfirm(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoAiAllowOneTapConfirm(enabled) }
    }

    /** 保存私有 AI 端点、模型和可选 API Key。 */
    fun saveAutoAiConfiguration(
        baseUrl: String,
        model: String,
        apiKey: String?,
        onSaved: () -> Unit = {},
    ) {
        launchWrite(
            action = {
                repository.updateAutoAiEndpoint(baseUrl, model)
                if (apiKey != null) AutoAiCredentialStore(repository).save(apiKey)
            },
            onSuccess = { onSaved() },
        )
    }

    /** 测试当前私有 AI 连接与兼容响应格式。 */
    fun testAutoAiConnection(onResult: (Boolean) -> Unit) {
        launchWrite(
            action = {
                PrivateAutoBookkeepingAiClient(repository, AutoAiCredentialStore(repository)).testConnection()
            },
            onSuccess = onResult,
        )
    }

    /** 校验并原子导入不超过 1MiB 的声明式规则包。 */
    fun importAutoRulePack(context: Context, bytes: ByteArray, onImported: () -> Unit = {}) {
        launchWrite(
            action = {
                AutoBookkeepingRuleEngine(context.applicationContext, repository).importRulePack(bytes)
            },
            onSuccess = { onImported() },
        )
    }

    /** 读取管理页面展示所需的内置规则包。 */
    fun readBuiltinAutoRulePack(context: Context): RulePackV1? = runCatching {
        AutoBookkeepingRuleEngine(context.applicationContext, repository).readBuiltinRulePack()
    }.getOrNull()

    /** 读取管理页面展示所需的用户规则包。 */
    fun readStoredAutoRulePack(context: Context, entity: AutoRulePackEntity): RulePackV1? = runCatching {
        AutoBookkeepingRuleEngine(context.applicationContext, repository).readStoredRulePack(entity)
    }.getOrNull()

    /** 启用或停用已经导入并通过校验的规则包。 */
    fun updateAutoRulePackActive(
        context: Context,
        entity: AutoRulePackEntity,
        active: Boolean,
        onUpdated: () -> Unit = {},
    ) {
        launchWrite(
            action = {
                AutoBookkeepingRuleEngine(context.applicationContext, repository)
                    .updateRulePackActive(entity, active)
            },
            onSuccess = { onUpdated() },
        )
    }

    /** 删除指定标识的全部用户规则包版本。 */
    fun deleteAutoRulePack(packId: String, onDeleted: () -> Unit = {}) {
        launchWrite(
            action = { repository.deleteAutoRulePack(packId) },
            onSuccess = { onDeleted() },
        )
    }

    /** 删除全部用户导入规则并恢复内置规则。 */
    fun restoreBuiltinAutoRules(onRestored: () -> Unit = {}) {
        launchWrite(
            action = repository::restoreBuiltinAutoRules,
            onSuccess = { onRestored() },
        )
    }

    /** 测试 Root 是否允许固定截图命令。 */
    fun testRootOcrAccess(onResult: (Boolean) -> Unit) {
        launchWrite(
            action = ::testRootScreenshotAccess,
            onSuccess = onResult,
        )
    }

    /** 一键确认已经达到高置信度的待确认账单。 */
    fun confirmAutoBookkeepingEvent(eventId: Long, onConfirmed: () -> Unit = {}) {
        launchWrite(
            action = { repository.confirmAutoBookkeepingEvent(eventId) },
            onSuccess = { onConfirmed() },
        )
    }

    /** 保存用户补全后的自动账单并学习本地映射。 */
    fun confirmAutoBookkeepingEvent(
        eventId: Long,
        draft: TransactionDraft,
        onConfirmed: () -> Unit,
    ) {
        launchWrite(
            action = { repository.confirmAutoBookkeepingEvent(eventId, draft) },
            onSuccess = { onConfirmed() },
        )
    }

    /** 忽略待确认账单并保留短期防重记录。 */
    fun ignoreAutoBookkeepingEvent(eventId: Long, onIgnored: () -> Unit = {}) {
        launchWrite(
            action = { repository.ignoreAutoBookkeepingEvent(eventId) },
            onSuccess = { onIgnored() },
        )
    }

    /**
     * 保存用户已经确认的账目草稿。
     */
    fun saveTransaction(
        draft: TransactionDraft,
        onSaved: () -> Unit,
    ) {
        launchWrite(
            action = { repository.saveTransaction(draft) },
            onSuccess = { onSaved() },
        )
    }

    /**
     * 更新用户确认修改后的已有账目。
     */
    fun updateTransaction(
        transactionId: Long,
        draft: TransactionDraft,
        onSaved: () -> Unit,
    ) {
        launchWrite(
            action = { repository.updateTransaction(transactionId, draft) },
            onSuccess = { onSaved() },
        )
    }

    /**
     * 删除指定账目并在完成后通知页面返回。
     */
    fun deleteTransaction(
        transactionId: Long,
        onDeleted: () -> Unit,
    ) {
        launchWrite(
            action = { repository.deleteTransaction(transactionId) },
            onSuccess = { onDeleted() },
        )
    }

    /**
     * 新增或更新账户并在完成后关闭编辑界面。
     */
    fun saveAccount(
        account: AccountEntity,
        ledgerIds: Set<Long>,
        onSaved: () -> Unit,
    ) {
        launchWrite(
            action = { repository.saveAccount(account, ledgerIds) },
            onSuccess = { onSaved() },
        )
    }

    /** 新增账本并在成功后返回标识。 */
    fun addLedger(
        name: String,
        coverKey: String,
        useLightText: Boolean,
        baseCurrencyKey: String,
        isHidden: Boolean,
        onAdded: (Long) -> Unit,
    ) {
        launchWrite(
            action = { repository.addLedger(name, coverKey, useLightText, baseCurrencyKey, isHidden) },
            onSuccess = onAdded,
        )
    }

    /** 更新账本信息。 */
    fun updateLedger(ledger: LedgerEntity, onUpdated: () -> Unit) {
        launchWrite(
            action = { repository.updateLedger(ledger) },
            onSuccess = { onUpdated() },
        )
    }

    /** 切换当前账本。 */
    fun selectLedger(ledgerId: Long, onSelected: () -> Unit) {
        pendingLedgerId = ledgerId
        pendingLedgerSelection = onSelected
        if (ledgerSelectionJob?.isActive == true) return
        ledgerSelectionJob = viewModelScope.launch {
            writeState.value = AccountingWriteState(inProgress = true)
            try {
                while (pendingLedgerId != null) {
                    val targetLedgerId = pendingLedgerId ?: break
                    val selected = pendingLedgerSelection ?: {}
                    pendingLedgerId = null
                    pendingLedgerSelection = null
                    repository.selectLedger(targetLedgerId)
                    if (pendingLedgerId == null) selected()
                }
                writeState.value = AccountingWriteState()
            } catch (error: CancellationException) {
                writeState.value = AccountingWriteState()
                throw error
            } catch (error: Exception) {
                val message = error.message?.takeIf(String::isNotBlank) ?: "操作失败，请重试"
                writeState.value = AccountingWriteState(error = message)
            } finally {
                ledgerSelectionJob = null
            }
        }
    }

    /** 删除没有明细的账本。 */
    fun deleteLedger(ledgerId: Long, onDeleted: () -> Unit) {
        launchWrite(
            action = { repository.deleteLedger(ledgerId) },
            onSuccess = { onDeleted() },
        )
    }

    /**
     * 新增自定义币种并返回其持久化标识。
     */
    fun addCurrency(
        name: String,
        symbol: String,
        rateToCnyScaled: Long,
        onAdded: (String) -> Unit,
    ) {
        launchWrite(
            action = { repository.addCurrency(name, symbol, rateToCnyScaled) },
            onSuccess = onAdded,
        )
    }

    /** 更新币种的可编辑信息与手动汇率。 */
    fun updateCurrency(
        currencyKey: String,
        name: String,
        symbol: String,
        rateToCnyScaled: Long,
        onUpdated: () -> Unit,
    ) {
        launchWrite(
            action = { repository.updateCurrency(currencyKey, name, symbol, rateToCnyScaled) },
            onSuccess = { onUpdated() },
        )
    }

    /** 删除未被账户使用的自定义币种。 */
    fun deleteCurrency(currencyKey: String, onDeleted: () -> Unit) {
        launchWrite(
            action = { repository.deleteCurrency(currencyKey) },
            onSuccess = { onDeleted() },
        )
    }

    /** 切换预置币种是否使用自动汇率。 */
    fun setCurrencyAutoRate(currencyKey: String, enabled: Boolean, onUpdated: () -> Unit) {
        launchWrite(
            action = { repository.setCurrencyAutoRate(currencyKey, enabled) },
            onSuccess = { onUpdated() },
        )
    }

    /**
     * 新增自定义账户类型并把新类型标识返回选择页。
     */
    fun addAccountType(
        name: String,
        summary: String,
        onAdded: (String) -> Unit,
    ) {
        launchWrite(
            action = { repository.addAccountType(name, summary) },
            onSuccess = onAdded,
        )
    }

    /**
     * 删除未被账户使用的自定义账户类型。
     */
    fun deleteAccountType(
        typeKey: String,
        onDeleted: () -> Unit,
    ) {
        launchWrite(
            action = { repository.deleteAccountType(typeKey) },
            onSuccess = { onDeleted() },
        )
    }

    /**
     * 更新自定义账户类型的名称与可选说明。
     */
    fun updateAccountType(
        typeKey: String,
        name: String,
        summary: String,
        onUpdated: () -> Unit,
    ) {
        launchWrite(
            action = { repository.updateAccountType(typeKey, name, summary) },
            onSuccess = { onUpdated() },
        )
    }

    /**
     * 停用指定账户并在完成后关闭编辑界面。
     */
    fun archiveAccount(
        accountId: Long,
        onArchived: () -> Unit,
    ) {
        launchWrite(
            action = { repository.archiveAccount(accountId) },
            onSuccess = { onArchived() },
        )
    }

    /** 删除没有历史账目的账户并在完成后关闭编辑界面。 */
    fun deleteAccount(
        accountId: Long,
        onDeleted: () -> Unit,
    ) {
        launchWrite(
            action = { repository.deleteAccount(accountId) },
            onSuccess = { onDeleted() },
        )
    }

    /**
     * 新增分类并返回新分类标识，重复名称直接通知页面。
     */
    fun addCategory(
        name: String,
        type: TransactionType,
        iconKey: String,
        onAdded: (Long) -> Unit,
        onDuplicate: () -> Unit,
    ) {
        launchWrite(
            action = { repository.addCategory(name, type, iconKey) },
            onSuccess = onAdded,
            onFailure = { message ->
                if (message == "同方向分类名称不能重复") {
                    onDuplicate()
                    true
                } else {
                    false
                }
            },
        )
    }

    /**
     * 串行执行一次写操作并统一更新进行状态、错误与成功回调。
     */
    private fun <T> launchWrite(
        action: suspend () -> T,
        onSuccess: (T) -> Unit,
        onFailure: (String) -> Boolean = { false },
    ) {
        if (writeState.value.inProgress) return
        viewModelScope.launch {
            writeState.value = AccountingWriteState(inProgress = true)
            try {
                val result = action()
                writeState.value = AccountingWriteState()
                onSuccess(result)
            } catch (error: CancellationException) {
                writeState.value = AccountingWriteState()
                throw error
            } catch (error: Exception) {
                val message = error.message?.takeIf(String::isNotBlank) ?: "操作失败，请重试"
                writeState.value = AccountingWriteState(
                    error = if (onFailure(message)) null else message,
                )
            }
        }
    }

    /** 更新分类名称与图标。 */
    fun updateCategory(
        categoryId: Long,
        name: String,
        iconKey: String,
        onUpdated: () -> Unit,
    ) {
        launchWrite(
            action = { repository.updateCategory(categoryId, name, iconKey) },
            onSuccess = { onUpdated() },
        )
    }

    /** 停用指定分类。 */
    fun archiveCategory(
        categoryId: Long,
        onArchived: () -> Unit,
    ) {
        launchWrite(
            action = { repository.archiveCategory(categoryId) },
            onSuccess = { onArchived() },
        )
    }

    /** 导出当前全部数据为加密备份字节。 */
    fun exportBackup(
        password: String,
        onReady: (ByteArray) -> Unit,
    ) {
        launchWrite(
            action = { backupManager.export(password) },
            onSuccess = onReady,
        )
    }

    /** 解密并校验备份文件，返回待恢复内容。 */
    fun parseBackup(
        blob: ByteArray,
        password: String,
        onParsed: (PreparedBackup) -> Unit,
    ) {
        launchWrite(
            action = { backupManager.parse(blob, password) },
            onSuccess = onParsed,
        )
    }

    /** 恢复备份：写入媒体并全量替换数据库。 */
    fun applyBackup(
        prepared: PreparedBackup,
        onDone: () -> Unit,
    ) {
        launchWrite(
            action = { backupManager.apply(prepared) },
            onSuccess = { onDone() },
        )
    }

    companion object {
        /**
         * 创建注入指定仓库的 ViewModel 工厂。
         */
        fun factory(
            repository: AccountingRepository,
            backupManager: BackupManager,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                /**
                 * 创建记账页面 ViewModel。
                 */
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AccountingViewModel(repository, backupManager) as T
            }
    }
}
