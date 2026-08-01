package com.vos.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 验证手动记账数字键盘的金额输入和一次加减计算。
 */
class ManualAmountInputTest {
    /**
     * 验证编辑已有账目时始终保留两位小数。
     */
    @Test
    fun fixedAmountTextKeepsTwoDecimals() {
        assertEquals("646.00", manualFixedAmountText(64_600))
        assertEquals("0.50", manualFixedAmountText(50))
    }


    /**
     * 验证数字键盘只保留两位小数。
     */
    @Test
    fun keepsTwoDecimalPlaces() {
        val expression = listOf("1", "2", ".", "3", "4", "5")
            .fold("", ::appendManualAmountKey)

        assertEquals("12.34", expression)
        assertEquals(1234L, calculateManualAmount(expression))
    }

    /**
     * 验证一次加法计算得到正确的最小货币单位。
     */
    @Test
    fun calculatesAddition() {
        val expression = listOf("1", "2", ".", "5", "+", "7", ".", "5")
            .fold("", ::appendManualAmountKey)

        assertEquals(2000L, calculateManualAmount(expression))
    }

    /**
     * 验证一次减法只接受大于零的最终金额。
     */
    @Test
    fun calculatesPositiveSubtraction() {
        val expression = listOf("1", "2", "−", "2", ".", "5")
            .fold("", ::appendManualAmountKey)

        assertEquals(950L, calculateManualAmount(expression))
        assertEquals(null, calculateManualAmount("2−3"))
    }
}
