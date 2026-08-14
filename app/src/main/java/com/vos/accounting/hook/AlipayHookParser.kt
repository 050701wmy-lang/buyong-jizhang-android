package com.vos.accounting.hook

import com.vos.accounting.model.TransactionType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** 表示支付宝 Hook 已解析、可提交给宿主应用的有限账单字段。 */
internal data class AlipayParsedCapture(
    val type: TransactionType,
    val amount: String,
    val merchant: String,
    val note: String = "",
    val paymentMethod: String = "",
    val externalTransactionId: String? = null,
    val occurredAt: Long,
)

/** 从支付宝同步消息和账单详情 DOM 中提取账单字段。 */
internal class AlipayHookParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** 从支付宝同步消息 JSON 中选择首个完整的成功交易。 */
    fun parseSync(rawJson: String): AlipayParsedCapture? {
        if (rawJson.isBlank() || rawJson.toByteArray().size > MAX_ALIPAY_SYNC_BYTES) return null
        val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() ?: return null
        return transactionCandidates(root).firstNotNullOfOrNull(::parseSyncCandidate)
    }

    /** 从账单 WebView 的编码 DOM 文本中提取一笔完整账单。 */
    fun parseDom(encodedText: String): AlipayParsedCapture? {
        val text = runCatching {
            (json.parseToJsonElement(encodedText) as? JsonPrimitive)?.contentOrNull
        }.getOrNull()?.trim().orEmpty()
        if (text.isEmpty() || ALIPAY_SUCCESS_KEYWORDS.none(text::contains)) return null
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        val type = detectAlipayType(text) ?: return null
        val amount = findHookAmount(text) ?: return null
        val externalId = findLabeledValue(lines, ALIPAY_EXTERNAL_ID_LABELS)
        val occurredAt = findLabeledValue(lines, ALIPAY_TIME_LABELS)?.let(::parseAlipayTime) ?: return null
        val merchant = findMerchantNearAmount(lines)
            ?: findLabeledValue(lines, ALIPAY_MERCHANT_LABELS)
            ?: return null
        return AlipayParsedCapture(
            type = type,
            amount = amount,
            merchant = merchant,
            note = findLabeledValue(lines, ALIPAY_NOTE_LABELS).orEmpty(),
            paymentMethod = findLabeledValue(lines, ALIPAY_PAYMENT_METHOD_LABELS).orEmpty(),
            externalTransactionId = externalId,
            occurredAt = occurredAt,
        )
    }

    /** 将一个同步消息对象转换为账单字段，非交易消息返回空。 */
    private fun parseSyncCandidate(candidate: JsonObject): AlipayParsedCapture? {
        val fields = flattenFields(candidate)
        val text = fields.values.joinToString("\n")
        if (!isSuccessfulAlipayMessage(text)) return null
        val type = detectAlipayType(text) ?: return null
        val amount = firstField(fields, ALIPAY_AMOUNT_KEYS)?.let(::normalizeHookAmount)
            ?: findHookAmount(text)
            ?: return null
        val occurredAt = firstField(fields, ALIPAY_TIME_KEYS)?.let(::parseAlipayTime)
            ?: System.currentTimeMillis()
        return AlipayParsedCapture(
            type = type,
            amount = amount,
            merchant = firstField(fields, ALIPAY_MERCHANT_KEYS)
                ?: findInlineLabeledValue(text, ALIPAY_MERCHANT_LABELS)
                ?: "",
            note = firstField(fields, ALIPAY_NOTE_KEYS).orEmpty(),
            paymentMethod = firstField(fields, ALIPAY_PAYMENT_METHOD_KEYS)
                ?: findInlineLabeledValue(text, ALIPAY_PAYMENT_METHOD_LABELS)
                ?: "",
            externalTransactionId = firstField(fields, ALIPAY_EXTERNAL_ID_KEYS),
            occurredAt = occurredAt,
        )
    }

    /** 返回根节点及嵌套对象中的有限交易候选。 */
    private fun transactionCandidates(root: JsonElement): List<JsonObject> = buildList {
        fun visit(element: JsonElement, depth: Int) {
            if (depth > MAX_ALIPAY_JSON_DEPTH) return
            when (element) {
                is JsonObject -> {
                    add(element)
                    element.values.forEach { visit(it, depth + 1) }
                }

                is JsonArray -> element.forEach { visit(it, depth + 1) }
                is JsonPrimitive -> element.contentOrNull
                    ?.takeIf { value -> value.startsWith('{') || value.startsWith('[') }
                    ?.let { value -> runCatching { json.parseToJsonElement(value) }.getOrNull() }
                    ?.let { nested -> visit(nested, depth + 1) }
            }
        }
        visit(root, 0)
    }

    /** 将对象字段名规范化后压平，嵌套字符串 JSON 同样参与解析。 */
    private fun flattenFields(root: JsonObject): Map<String, String> = buildMap {
        fun visit(element: JsonElement, key: String, depth: Int) {
            if (depth > MAX_ALIPAY_JSON_DEPTH) return
            when (element) {
                is JsonObject -> element.forEach { (childKey, child) -> visit(child, childKey, depth + 1) }
                is JsonArray -> element.forEach { child -> visit(child, key, depth + 1) }
                is JsonPrimitive -> {
                    val value = element.contentOrNull?.trim()?.takeIf(String::isNotEmpty) ?: return
                    putIfAbsent(normalizeAlipayKey(key), value)
                    if (value.startsWith('{') || value.startsWith('[')) {
                        runCatching { json.parseToJsonElement(value) }.getOrNull()
                            ?.let { nested -> visit(nested, key, depth + 1) }
                    }
                }
            }
        }
        visit(root, "", 0)
    }
}

/** 从规范化字段集合中读取首个已知键值。 */
private fun firstField(fields: Map<String, String>, keys: Set<String>): String? = keys
    .firstNotNullOfOrNull { key -> fields[normalizeAlipayKey(key)] }
    ?.trim()
    ?.takeIf(String::isNotEmpty)

/** 去掉字段键中的分隔符并统一小写。 */
private fun normalizeAlipayKey(value: String): String = value
    .lowercase()
    .filter(Char::isLetterOrDigit)

/** 判断同步消息是否表示已经完成的交易。 */
private fun isSuccessfulAlipayMessage(text: String): Boolean =
    ALIPAY_SUCCESS_KEYWORDS.any(text::contains) || ALIPAY_SUCCESS_CODES.any { code ->
        text.contains(code, ignoreCase = true)
    }

/** 判断支付宝文本对应的收支方向。 */
private fun detectAlipayType(text: String): TransactionType? = when {
    ALIPAY_INCOME_KEYWORDS.any(text::contains) -> TransactionType.INCOME
    ALIPAY_EXPENSE_KEYWORDS.any(text::contains) -> TransactionType.EXPENSE
    else -> null
}

/** 从换行文本读取标签同行或下一行的值。 */
private fun findLabeledValue(lines: List<String>, labels: List<String>): String? {
    lines.forEachIndexed { index, line ->
        labels.forEach { label ->
            if (line == label) return lines.getOrNull(index + 1)
            if (line.startsWith(label)) {
                val value = line.removePrefix(label).trimStart('：', ':', ' ')
                if (value.isNotEmpty()) return value
            }
        }
    }
    return null
}

/** 从未分行的同步消息文本读取标签值。 */
private fun findInlineLabeledValue(text: String, labels: List<String>): String? = labels
    .firstNotNullOfOrNull { label ->
        Regex("${Regex.escape(label)}[：:]?\\s*([^,，;；\\n\\r\\\"}]+)")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

/** 从金额附近选择不属于状态、标签或导航的商户名称。 */
private fun findMerchantNearAmount(lines: List<String>): String? {
    val amountIndex = lines.indexOfFirst { line -> findHookAmount(line) != null }
    if (amountIndex < 0) return null
    return sequenceOf(lines.getOrNull(amountIndex + 1), lines.getOrNull(amountIndex - 1))
        .filterNotNull()
        .firstOrNull { value ->
            value.length in 1..100 &&
                ALIPAY_DETAIL_NOISE.none(value::contains) &&
                value !in ALIPAY_DETAIL_LABELS &&
                findHookAmount(value) == null
        }
}

/** 将支付宝秒级、毫秒级时间戳或本地时间文本转换为毫秒。 */
private fun parseAlipayTime(value: String): Long? {
    val text = value.trim()
    text.toLongOrNull()?.let { timestamp ->
        return when {
            timestamp in 1 until MIN_ALIPAY_MILLISECOND_TIMESTAMP -> timestamp * 1_000L
            timestamp >= MIN_ALIPAY_MILLISECOND_TIMESTAMP -> timestamp
            else -> null
        }
    }
    return ALIPAY_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
        runCatching {
            LocalDateTime.parse(text, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    } ?: runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
}

private const val MAX_ALIPAY_SYNC_BYTES = 64 * 1024
private const val MAX_ALIPAY_JSON_DEPTH = 12
private const val MIN_ALIPAY_MILLISECOND_TIMESTAMP = 1_000_000_000_000L
private val ALIPAY_SUCCESS_KEYWORDS = listOf("交易成功", "支付成功", "付款成功", "收款成功", "收款到账", "退款成功")
private val ALIPAY_SUCCESS_CODES = listOf("TRADE_SUCCESS", "PAY_SUCCESS", "SUCCESS")
private val ALIPAY_INCOME_KEYWORDS =
    listOf("收款成功", "收款到账", "收入", "已收款", "退款成功", "退款到账", "收益发放", "收益到账")
private val ALIPAY_EXPENSE_KEYWORDS = listOf(
    "支付成功", "付款成功", "交易成功", "支出", "消费", "扣款", "TRADE_SUCCESS", "PAY_SUCCESS",
)
private val ALIPAY_AMOUNT_KEYS = setOf(
    "amount", "total_amount", "totalAmount", "trade_amount", "tradeAmount", "pay_amount", "payAmount",
    "buyer_pay_amount", "buyerPayAmount", "receipt_amount", "receiptAmount", "money", "price",
)
private val ALIPAY_MERCHANT_KEYS = setOf(
    "merchant_name", "merchantName", "seller_name", "sellerName", "counterparty", "payeeName", "targetName",
)
private val ALIPAY_NOTE_KEYS = setOf("subject", "title", "product_name", "productName", "goodsTitle", "remark", "memo")
private val ALIPAY_PAYMENT_METHOD_KEYS = setOf(
    "payment_method", "paymentMethod", "pay_tool", "payTool", "fundChannel", "channelName", "accountName",
)
private val ALIPAY_EXTERNAL_ID_KEYS = setOf(
    "trade_no", "tradeNo", "biz_no", "bizNo", "transaction_id", "transactionId", "order_no", "orderNo",
)
private val ALIPAY_TIME_KEYS = setOf(
    "gmt_payment", "gmtPayment", "pay_time", "payTime", "trade_time", "tradeTime", "create_time", "createTime",
    "notify_time", "notifyTime", "timestamp",
)
private val ALIPAY_MERCHANT_LABELS = listOf("收款方", "交易对象", "对方", "商户名称", "商户", "付款给")
private val ALIPAY_NOTE_LABELS = listOf("商品说明", "商品", "订单信息", "备注")
private val ALIPAY_PAYMENT_METHOD_LABELS = listOf("付款方式", "支付方式", "扣款方式", "资金渠道")
private val ALIPAY_EXTERNAL_ID_LABELS =
    listOf("支付宝交易号", "交易号", "交易单号", "订单号", "商户订单号", "交易详情")
private val ALIPAY_TIME_LABELS = listOf("付款时间", "支付时间", "交易时间", "创建时间", "收款时间")
private val ALIPAY_DETAIL_LABELS =
    ALIPAY_MERCHANT_LABELS + ALIPAY_NOTE_LABELS + ALIPAY_PAYMENT_METHOD_LABELS +
        ALIPAY_EXTERNAL_ID_LABELS + ALIPAY_TIME_LABELS
private val ALIPAY_DETAIL_NOISE = listOf("交易成功", "支付成功", "付款成功", "收款成功", "交易详情", "账单详情")
private val ALIPAY_TIME_FORMATTERS = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"),
)
