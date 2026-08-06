package com.vos.accounting.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

/**
 * 表示账目的收支方向，转账流水不参与普通收支统计。
 */
@Serializable
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
}

/**
 * 表示转账流水在账户中的出入方向。
 */
@Serializable
enum class TransferDirection {
    IN,
    OUT,
}

/**
 * 表示账目的创建来源。
 */
@Serializable
enum class TransactionSource {
    MANUAL,
    AI,
}

/**
 * 表示账户类型。
 */
@Serializable
enum class AccountType {
    CASH,
    BANK_CARD,
    CREDIT,
    ONLINE,
    INVESTMENT,
    STORED_VALUE,
    VIRTUAL,
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
    val ledgerId: Long = 1,
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
