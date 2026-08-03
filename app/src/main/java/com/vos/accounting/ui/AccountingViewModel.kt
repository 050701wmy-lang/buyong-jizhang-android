package com.vos.accounting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vos.accounting.ai.AiBookkeepingParser
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.CategoryTotal
import com.vos.accounting.model.OverviewTotals
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
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
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val totals: OverviewTotals = OverviewTotals(0, 0),
    val expenseCategoryTotals: List<CategoryTotal> = emptyList(),
    val aiDraft: TransactionDraft? = null,
    val aiError: String? = null,
    val writeInProgress: Boolean = false,
    val writeError: String? = null,
    val themeMode: AccountingThemeMode = AccountingThemeMode.SYSTEM,
    val followSystemColor: Boolean = true,
)

/**
 * 管理账本界面状态、AI 草稿和统一保存动作。
 */
class AccountingViewModel(
    private val repository: AccountingRepository,
    private val aiParser: AiBookkeepingParser = AiBookkeepingParser(),
) : ViewModel() {
    private val aiDraft = MutableStateFlow<TransactionDraft?>(null)
    private val aiError = MutableStateFlow<String?>(null)
    private val writeState = MutableStateFlow(AccountingWriteState())

    private val accountState = combine(
        repository.accounts,
        repository.accountTypes,
        repository.currencies,
    ) { accounts, accountTypes, currencies ->
        Triple(accounts, accountTypes, currencies)
    }

    private val ledgerState = combine(
        accountState,
        repository.categories,
        repository.transactions,
        repository.overviewTotals,
        repository.expenseCategoryTotals,
    ) { accountState, categories, transactions, totals, categoryTotals ->
        AccountingUiState(
            accounts = accountState.first,
            accountTypes = accountState.second,
            currencies = accountState.third,
            categories = categories,
            transactions = transactions,
            totals = totals,
            expenseCategoryTotals = categoryTotals,
        )
    }

    val uiState = combine(
        ledgerState,
        repository.settings,
        aiDraft,
        aiError,
        writeState,
    ) { ledger, settings, draft, error, write ->
        ledger.copy(
            aiDraft = draft,
            aiError = error,
            writeInProgress = write.inProgress,
            writeError = write.error,
            themeMode = settings?.themeMode?.let { mode ->
                AccountingThemeMode.entries.firstOrNull { it.name == mode }
            } ?: AccountingThemeMode.SYSTEM,
            followSystemColor = settings?.followSystemColor ?: true,
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
     * 使用自然语言生成一份尚未入账的 AI 草稿。
     */
    fun createAiDraft(text: String) {
        val state = uiState.value
        try {
            aiDraft.value = aiParser.parse(
                text = text,
                accounts = state.accounts.filterNot(AccountEntity::isArchived),
                categories = state.categories.filterNot(CategoryEntity::isArchived),
                occurredAt = System.currentTimeMillis(),
            )
            aiError.value = null
        } catch (error: IllegalArgumentException) {
            aiDraft.value = null
            aiError.value = error.message
        }
    }

    /**
     * 清空上一次 AI 解析产生的临时状态。
     */
    fun clearAiDraft() {
        aiDraft.value = null
        aiError.value = null
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
     * 保存用户已经确认的手动或 AI 草稿。
     */
    fun saveTransaction(
        draft: TransactionDraft,
        onSaved: () -> Unit,
    ) {
        launchWrite(
            action = { repository.saveTransaction(draft) },
            onSuccess = {
                clearAiDraft()
                onSaved()
            },
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
        onSaved: () -> Unit,
    ) {
        launchWrite(
            action = { repository.saveAccount(account) },
            onSuccess = { onSaved() },
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

    companion object {
        /**
         * 创建注入指定仓库的 ViewModel 工厂。
         */
        fun factory(repository: AccountingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                /**
                 * 创建记账页面 ViewModel。
                 */
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AccountingViewModel(repository) as T
            }
    }
}
