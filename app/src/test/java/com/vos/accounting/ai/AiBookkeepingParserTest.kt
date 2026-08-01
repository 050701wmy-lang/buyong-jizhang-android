package com.vos.accounting.ai

import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 验证自然语言记账草稿的金额、方向与分类识别。
 */
class AiBookkeepingParserTest {
    private val accounts = listOf(
        AccountEntity(
            id = 1,
            name = "现金",
            type = AccountType.CASH,
            openingBalanceMinor = 0,
            sortOrder = 0,
        ),
    )
    private val categories = listOf(
        CategoryEntity(id = 1, name = "餐饮", type = TransactionType.EXPENSE, sortOrder = 0),
        CategoryEntity(id = 2, name = "交通", type = TransactionType.EXPENSE, sortOrder = 1),
        CategoryEntity(id = 3, name = "工资", type = TransactionType.INCOME, sortOrder = 0),
    )
    private val parser = AiBookkeepingParser()

    /**
     * 验证日常支出可识别为最小货币单位和餐饮分类。
     */
    @Test
    fun parseMealExpense() {
        val draft = parser.parse(
            text = "午饭 25.50 元",
            accounts = accounts,
            categories = categories,
            occurredAt = 1000,
        )

        assertEquals(2550L, draft.amountMinor)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(1, draft.categoryId)
        assertEquals(TransactionSource.AI, draft.source)
    }

    /**
     * 验证工资文本可识别为收入。
     */
    @Test
    fun parseSalaryIncome() {
        val draft = parser.parse(
            text = "工资到账 8000 元",
            accounts = accounts,
            categories = categories,
            occurredAt = 1000,
        )

        assertEquals(800_000L, draft.amountMinor)
        assertEquals(TransactionType.INCOME, draft.type)
        assertEquals(3, draft.categoryId)
    }
}
