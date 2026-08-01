package com.vos.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 验证金额输入始终转换为精确的最小货币单位。
 */
class FormattersTest {
    /**
     * 验证两位小数金额可精确转换。
     */
    @Test
    fun parseTwoDecimalMoney() {
        assertEquals(1234L, parseMoneyToMinor("12.34"))
    }

    /**
     * 验证超过两位小数的输入不会进入账本。
     */
    @Test
    fun rejectExcessiveDecimals() {
        assertNull(parseMoneyToMinor("12.345"))
    }
}
