package com.vos.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 验证金额输入始终转换为精确的最小货币单位。
 */
class FormattersTest {
    /**
     * 验证同一时间戳在不同时区会归入正确的自然日。
     */
    @Test
    fun transactionLocalDateUsesProvidedZone() {
        val timestamp = Instant.parse("2026-08-01T16:30:00Z").toEpochMilli()

        assertEquals(
            LocalDate.of(2026, 8, 2),
            transactionLocalDate(timestamp, ZoneId.of("Asia/Shanghai")),
        )
        assertEquals(
            LocalDate.of(2026, 8, 1),
            transactionLocalDate(timestamp, ZoneId.of("America/Los_Angeles")),
        )
    }

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
