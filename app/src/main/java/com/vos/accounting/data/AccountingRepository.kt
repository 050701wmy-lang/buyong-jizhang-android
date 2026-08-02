package com.vos.accounting.data

import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransactionDraft

/**
 * 表示用户可修正的账务写入错误。
 */
class AccountingWriteException(message: String) : IllegalArgumentException(message)

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
        validateTransactionDraft(draft)
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
        val original = dao.findTransaction(transactionId)
            ?: throw AccountingWriteException("账目不存在或已被删除")
        validateTransactionDraft(draft, original)
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
    suspend fun saveAccount(account: AccountEntity): Long {
        val normalized = account.copy(
            name = account.name.trim(),
            sortOrder = if (account.id == 0L) dao.maxAccountSortOrder() + 1 else account.sortOrder,
            isDefault = account.isDefault && !account.isArchived,
        )
        if (normalized.name.isEmpty()) throw AccountingWriteException("账户名称不能为空")
        return dao.saveAccount(normalized)
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
    private suspend fun validateTransactionDraft(
        draft: TransactionDraft,
        original: TransactionEntity? = null,
    ) {
        if (draft.amountMinor <= 0) throw AccountingWriteException("金额必须大于零")
        if (draft.occurredAt <= 0) throw AccountingWriteException("记账时间无效")
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
    }
}
