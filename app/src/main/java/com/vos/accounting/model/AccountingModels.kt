package com.vos.accounting.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

/** 单笔账目与账户余额的金额上限（最小货币单位），对应 99,999,999.99 元。 */
const val MAX_AMOUNT_MINOR = 99_999_999_999L

/** 自定义币种兑人民币汇率的业务上限（定点值），对应 10,000 元。 */
const val MAX_RATE_TO_CNY_SCALED = 1_000_000_000_000L

/**
 * 表示账目的收支方向。
 */
@Serializable
enum class TransactionType {
    EXPENSE,
    INCOME,
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
    val currencyKey: String = "",
    val accountAmountMinor: Long = 0,
    val accountId: Long,
    val categoryId: Long?,
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
