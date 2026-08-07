package com.vos.accounting.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * 将最小货币单位格式化为不带货币符号的两位小数文本。
 */
fun formatDecimalAmount(amountMinor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.CHINA)
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.format(BigDecimal.valueOf(amountMinor, 2))
}

/**
 * 使用账户币种符号格式化最小货币单位金额。
 */
fun formatCurrencyAmount(amountMinor: Long, symbol: String): String =
    "$symbol${formatDecimalAmount(amountMinor)}"

/**
 * 将用户输入的十进制金额解析为最小货币单位。
 */
fun parseMoneyToMinor(text: String): Long? {
    val amount = text.trim().toBigDecimalOrNull() ?: return null
    if (amount <= BigDecimal.ZERO || amount.scale() > 2) return null
    val minor = amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
    if (minor > BigDecimal.valueOf(Long.MAX_VALUE)) return null
    return minor.longValueExact()
}

/**
 * 返回时间戳在指定时区对应的自然日。
 */
fun transactionLocalDate(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
