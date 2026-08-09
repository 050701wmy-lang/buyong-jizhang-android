package com.vos.accounting.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL

const val CURRENCY_RATE_SCALE = 100_000_000L

/** 自动汇率成功刷新后的缓存有效期。 */
const val RATE_REFRESH_TTL_MILLIS = 24L * 60 * 60 * 1000

/**
 * 使用定点汇率把账户最小货币单位换算为人民币分。
 */
fun convertToCnyMinor(amountMinor: Long, rateToCnyScaled: Long): Long = BigDecimal
    .valueOf(amountMinor)
    .multiply(BigDecimal.valueOf(rateToCnyScaled))
    .divide(BigDecimal.valueOf(CURRENCY_RATE_SCALE), 0, RoundingMode.HALF_UP)
    .longValueExact()

/** 把来源币种最小单位金额换算为目标币种最小单位金额。 */
fun convertCurrencyMinor(amountMinor: Long, sourceRateToCnyScaled: Long, targetRateToCnyScaled: Long): Long = BigDecimal
    .valueOf(amountMinor)
    .multiply(BigDecimal.valueOf(sourceRateToCnyScaled))
    .divide(BigDecimal.valueOf(targetRateToCnyScaled), 0, RoundingMode.HALF_UP)
    .longValueExact()

/**
 * 从免密汇率服务读取以人民币为基准的公开汇率。
 */
fun fetchCurrencyRates(codes: Set<String>): Map<String, Long> = runCatching {
    val connection = URL("https://open.er-api.com/v6/latest/CNY").openConnection() as HttpURLConnection
    connection.connectTimeout = 8_000
    connection.readTimeout = 8_000
    connection.requestMethod = "GET"
    connection.inputStream.bufferedReader().use { reader ->
        val root = Json.parseToJsonElement(reader.readText()).jsonObject
        if (root.getValue("result").jsonPrimitive.content != "success") return emptyMap()
        val rates = root.getValue("rates").jsonObject
        codes.associateWith { code ->
            if (code == "CNY") {
                CURRENCY_RATE_SCALE
            } else {
                BigDecimal.ONE
                    .divide(rates.getValue(code).jsonPrimitive.content.toBigDecimal(), 12, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(CURRENCY_RATE_SCALE))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            }
        }
    }
}.getOrDefault(emptyMap())
