package com.vos.accounting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vos.accounting.ai.AiBookkeepingParser
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.CategoryTotal
import com.vos.accounting.model.OverviewTotals
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 汇总所有主页面需要的账本状态。
 */
data class AccountingUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val totals: OverviewTotals = OverviewTotals(0, 0),
    val expenseCategoryTotals: List<CategoryTotal> = emptyList(),
    val aiDraft: TransactionDraft? = null,
    val aiError: String? = null,
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

    private val ledgerState = combine(
        repository.accounts,
        repository.categories,
        repository.transactions,
        repository.overviewTotals,
        repository.expenseCategoryTotals,
    ) { accounts, categories, transactions, totals, categoryTotals ->
        AccountingUiState(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            totals = totals,
            expenseCategoryTotals = categoryTotals,
        )
    }

    val uiState = combine(ledgerState, aiDraft, aiError) { ledger, draft, error ->
        ledger.copy(aiDraft = draft, aiError = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountingUiState(),
    )

    init {
        viewModelScope.launch {
            repository.initialize()
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
     * 保存用户已经确认的手动或 AI 草稿。
     */
    fun saveTransaction(
        draft: TransactionDraft,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.saveTransaction(draft)
            clearAiDraft()
            onSaved()
        }
    }

    /**
     * 更新用户确认修改后的已有账目。
     */
    fun updateTransaction(
        transactionId: Long,
        draft: TransactionDraft,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.updateTransaction(transactionId, draft)
            onSaved()
        }
    }

    /**
     * 删除指定账目并在完成后通知页面返回。
     */
    fun deleteTransaction(
        transactionId: Long,
        onDeleted: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            onDeleted()
        }
    }

    /**
     * 新增或更新账户并在完成后关闭编辑界面。
     */
    fun saveAccount(
        account: AccountEntity,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.saveAccount(account)
            onSaved()
        }
    }

    /**
     * 停用指定账户并在完成后关闭编辑界面。
     */
    fun archiveAccount(
        accountId: Long,
        onArchived: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.archiveAccount(accountId)
            onArchived()
        }
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
        val normalizedName = name.trim()
        if (uiState.value.categories.any {
                it.type == type && it.name == normalizedName
            }
        ) {
            onDuplicate()
            return
        }
        viewModelScope.launch {
            onAdded(repository.addCategory(normalizedName, type, iconKey))
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
