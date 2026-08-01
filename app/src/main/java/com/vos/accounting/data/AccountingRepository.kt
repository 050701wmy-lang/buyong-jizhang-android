package com.vos.accounting.data

import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransactionDraft

/**
 * 汇总账本数据读取与统一写入流程。
 */
class AccountingRepository(
    private val dao: AccountingDao,
) {
    val accounts = dao.observeAccounts()
    val categories = dao.observeCategories()
    val transactions = dao.observeTransactions()
    val overviewTotals = dao.observeOverviewTotals()
    val expenseCategoryTotals = dao.observeExpenseCategoryTotals()

    /**
     * 建立首次启动所需的默认账本数据。
     */
    suspend fun initialize() {
        dao.seedDefaults()
    }

    /**
     * 把已经确认的草稿写入正式账目。
     */
    suspend fun saveTransaction(draft: TransactionDraft): Long {
        require(draft.amountMinor > 0)
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
        require(draft.amountMinor > 0)
        dao.updateTransaction(
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
            ),
        )
    }

    /**
     * 删除指定账目。
     */
    suspend fun deleteTransaction(transactionId: Long) {
        dao.deleteTransaction(transactionId)
    }

    /**
     * 新增或更新一个账户。
     */
    suspend fun saveAccount(account: AccountEntity): Long {
        val normalized = account.copy(
            name = account.name.trim(),
            sortOrder = if (account.id == 0L) dao.maxAccountSortOrder() + 1 else account.sortOrder,
            isDefault = account.isDefault && !account.isArchived,
        )
        require(normalized.name.isNotEmpty())
        return dao.saveAccount(normalized)
    }

    /**
     * 停用指定账户。
     */
    suspend fun archiveAccount(accountId: Long) {
        dao.archiveAccount(accountId)
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
        require(normalizedName.isNotEmpty())
        return dao.insertCategory(
            CategoryEntity(
                name = normalizedName,
                type = type,
                sortOrder = dao.maxCategorySortOrder(type) + 1,
                iconKey = iconKey,
            ),
        )
    }
}
