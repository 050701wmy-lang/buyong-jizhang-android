package com.vos.accounting.model

import androidx.room.ColumnInfo

/**
 * 表示账目的收支方向。
 */
enum class TransactionType {
    EXPENSE,
    INCOME,
}

/**
 * 表示账目的创建来源。
 */
enum class TransactionSource {
    MANUAL,
    AI,
}

/**
 * 表示账户类型。
 */
enum class AccountType {
    CASH,
    BANK_CARD,
    ONLINE,
}

/**
 * 承载新建账目在确认前的完整数据。
 */
data class TransactionDraft(
    val type: TransactionType,
    val amountMinor: Long,
    val accountId: Long,
    val categoryId: Long,
    val merchant: String,
    val note: String,
    val occurredAt: Long,
    val source: TransactionSource,
)

/**
 * 承载首页收支汇总。
 */
data class OverviewTotals(
    @ColumnInfo(name = "income_minor")
    val incomeMinor: Long,
    @ColumnInfo(name = "expense_minor")
    val expenseMinor: Long,
) {
    /**
     * 计算当前收支结余。
     */
    val balanceMinor: Long
        get() = incomeMinor - expenseMinor
}

/**
 * 承载分类统计结果。
 */
data class CategoryTotal(
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
)
