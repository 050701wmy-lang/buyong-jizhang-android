package com.vos.accounting.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 将最小货币单位格式化为人民币文本。
 */
fun formatMoney(amountMinor: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)
    return formatter.format(BigDecimal.valueOf(amountMinor, 2))
}

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
 * 将时间戳格式化为适合账目列表的日期时间。
 */
fun formatTransactionTime(epochMillis: Long): String = Instant
    .ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

/**
 * 返回时间戳在指定时区对应的自然日。
 */
fun transactionLocalDate(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
