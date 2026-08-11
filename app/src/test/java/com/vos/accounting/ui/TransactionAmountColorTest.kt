package com.vos.accounting.ui

import androidx.compose.ui.graphics.Color
import com.vos.accounting.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/** 验证收支颜色开关。 */
class TransactionAmountColorTest {
    /** 开启时支出和收入使用红绿字体。 */
    @Test
    fun enabled_colors_income_and_expense_only() {
        val defaultColor = Color.Black

        assertEquals(EXPENSE_AMOUNT_COLOR, transactionAmountColor(TransactionType.EXPENSE, true, defaultColor))
        assertEquals(INCOME_AMOUNT_COLOR, transactionAmountColor(TransactionType.INCOME, true, defaultColor))
        assertEquals(defaultColor, transactionAmountColor(TransactionType.EXPENSE, false, defaultColor))
    }
}
