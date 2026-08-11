package com.vos.accounting.hook

import com.vos.accounting.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** 表示 Kinda 支付页在内存中暂存的有限账单字段。 */
internal data class WechatCachedFields(
    val amount: String? = null,
    val merchant: String? = null,
    val paymentMethod: String? = null,
)

/** 表示微信 Hook 已解析、可提交给宿主应用的有限账单字段。 */
internal data class WechatParsedCapture(
    val type: TransactionType,
    val amount: String,
    val merchant: String,
    val note: String = "",
    val paymentMethod: String,
    val externalTransactionId: String?,
    val occurredAt: Long,
)

/** 表示从微信消息表插入参数中选出的有限字段。 */
internal data class WechatMessageRecord(
    val table: String,
    val type: Long?,
    val isSend: Int?,
    val createTime: Long?,
    val fields: Map<String, String>,
)

/** 独立解析微信 XWeb 回调和消息表记录，不依赖具体微信版本号。 */
internal class WechatHookParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** 从微信支付 XWeb 回调中提取账单，并仅用缓存补齐空字段。 */
    fun parseXWeb(script: String, cached: WechatCachedFields): WechatParsedCapture? {
        val response = readXWebResponse(script) ?: return null
        val header = objectValue(response["header"])
        val rawFee = stringValue(header?.get("fee"))
        val amount = normalizeHookAmount(rawFee) ?: cached.amount ?: return null
        val preview = response["preview"] as? JsonArray
        val previewFields = readPreviewFields(preview)
        val joined = buildString {
            append(rawFee.orEmpty())
            previewFields.forEach { (label, value) -> append(' ').append(label).append(' ').append(value) }
        }
        val type = when {
            rawFee?.trim()?.startsWith('+') == true -> TransactionType.INCOME
            rawFee?.trim()?.startsWith('-') == true -> TransactionType.EXPENSE
            WECHAT_INCOME_KEYWORDS.any(joined::contains) -> TransactionType.INCOME
            WECHAT_EXPENSE_KEYWORDS.any(joined::contains) -> TransactionType.EXPENSE
            else -> return null
        }
        val occurredAt = parseWechatTime(previewValue(previewFields, WECHAT_TIME_LABELS))
            ?: findWechatTime(response)
            ?: return null
        return WechatParsedCapture(
            type = type,
            amount = amount,
            merchant = stringValue(header?.get("nickname"))
                .orEmpty()
                .ifEmpty { previewValue(previewFields, WECHAT_MERCHANT_LABELS).orEmpty() }
                .ifEmpty { cached.merchant.orEmpty() },
            paymentMethod = previewValue(previewFields, WECHAT_PAYMENT_METHOD_LABELS)
                .orEmpty()
                .ifEmpty { cached.paymentMethod.orEmpty() },
            externalTransactionId = previewValue(previewFields, WECHAT_EXTERNAL_ID_LABELS),
            occurredAt = occurredAt,
        )
    }

    /** 从详情页 DOM 文本中提取完整账单，缺少任一关键详情字段时拒绝识别。 */
    fun parseXWebDom(encodedText: String): WechatParsedCapture? {
        val text = runCatching {
            (json.parseToJsonElement(encodedText) as? JsonPrimitive)?.contentOrNull
        }.getOrNull() ?: return null
        val status = findLabeledValue(text, WECHAT_STATUS_LABELS) ?: return null
        if (WECHAT_DETAIL_SUCCESS_KEYWORDS.none(status::contains)) return null
        val amountMatch = WECHAT_DOM_AMOUNT_PATTERN.find(text) ?: return null
        val type = if (amountMatch.groupValues[1] in WECHAT_DOM_EXPENSE_SIGNS) {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }
        val amount = normalizeHookAmount(amountMatch.groupValues[2]) ?: return null
        val occurredAt = WECHAT_TIME_PATTERN.find(text)?.value?.let(::parseWechatTime) ?: return null
        val externalId = findLabeledValue(text, WECHAT_EXTERNAL_ID_LABELS) ?: return null
        val merchant = text.substring(0, amountMatch.range.first)
            .lineSequence()
            .map(String::trim)
            .lastOrNull(String::isNotEmpty)
            ?: return null
        return WechatParsedCapture(
            type = type,
            amount = amount,
            merchant = merchant,
            note = findLabeledValue(text, WECHAT_PRODUCT_LABELS).orEmpty(),
            paymentMethod = findLabeledValue(text, WECHAT_PAYMENT_METHOD_LABELS).orEmpty(),
            externalTransactionId = externalId,
            occurredAt = occurredAt,
        )
    }

    /** 从微信 WCDB 消息记录中提取账单，并仅用缓存补齐空字段。 */
    fun parseMessage(record: WechatMessageRecord, cached: WechatCachedFields): WechatParsedCapture? {
        if (!isSupportedMessageRecord(record)) return null
        val raw = record.fields.values.joinToString("\n")
        val text = stripMarkup(raw)
        val type = when {
            WECHAT_INCOME_KEYWORDS.any(text::contains) -> TransactionType.INCOME
            WECHAT_EXPENSE_KEYWORDS.any(text::contains) -> TransactionType.EXPENSE
            record.isSend == 1 -> TransactionType.EXPENSE
            record.isSend == 0 -> TransactionType.INCOME
            else -> return null
        }
        val amount = findHookAmount(text) ?: findHookAmount(raw) ?: cached.amount ?: return null
        val merchant = findXmlValue(raw, WECHAT_MERCHANT_XML_TAGS)
            ?: findLabeledValue(text, WECHAT_MERCHANT_LABELS)
            ?: cached.merchant
            ?: ""
        val paymentMethod = findXmlValue(raw, WECHAT_PAYMENT_METHOD_XML_TAGS)
            ?: findLabeledValue(text, WECHAT_PAYMENT_METHOD_LABELS)
            ?: cached.paymentMethod
            ?: ""
        val externalId = findXmlValue(raw, WECHAT_EXTERNAL_ID_XML_TAGS)
            ?: findLabeledValue(text, WECHAT_EXTERNAL_ID_LABELS)
        return WechatParsedCapture(
            type = type,
            amount = amount,
            merchant = merchant,
            paymentMethod = paymentMethod,
            externalTransactionId = externalId,
            occurredAt = record.createTime?.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    /** 把 JSON 对象文本安全解析为对象。 */
    private fun parseObject(text: String): JsonObject? = runCatching {
        json.parseToJsonElement(text) as? JsonObject
    }.getOrNull()

    /** 从微信桥接脚本中读取结构化支付响应。 */
    private fun readXWebResponse(script: String): JsonObject? {
        if (!script.contains(XWEB_SUCCESS_MARKER)) return null
        val outerText = script.substringAfter(XWEB_CALLBACK_MARKER, "").substringBeforeLast(')', "")
        val outer = parseObject(outerText) ?: return null
        val message = objectValue(outer["__json_message"]) ?: return null
        val params = objectValue(message["__params"]) ?: return null
        return objectValue(params["respbuf"])
    }

    /** 读取可直接为对象或以字符串承载对象的 JSON 字段。 */
    private fun objectValue(element: JsonElement?): JsonObject? = when (element) {
        is JsonObject -> element
        is JsonPrimitive -> element.contentOrNull?.let(::parseObject)
        else -> null
    }

    /** 读取 JSON 原始字符串值。 */
    private fun stringValue(element: JsonElement?): String? = (element as? JsonPrimitive)
        ?.contentOrNull
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    /** 把微信预览数组压平成标签和值。 */
    private fun readPreviewFields(preview: JsonArray?): List<Pair<String, String>> = preview
        .orEmpty()
        .mapNotNull { item ->
            val objectItem = item as? JsonObject ?: return@mapNotNull null
            val label = readNamedValue(objectItem["label"]) ?: return@mapNotNull null
            val values = objectItem["value"] as? JsonArray
            val value = values?.firstNotNullOfOrNull(::readNamedValue) ?: return@mapNotNull null
            label to value
        }

    /** 读取预览字段中对象的 name 或原始字符串。 */
    private fun readNamedValue(element: JsonElement?): String? = when (element) {
        is JsonObject -> stringValue(element["name"]) ?: stringValue(element["value"])
        is JsonPrimitive -> stringValue(element)
        else -> null
    }

    /** 按标签集合读取首个预览值。 */
    private fun previewValue(fields: List<Pair<String, String>>, labels: List<String>): String? = fields
        .firstOrNull { (label, _) -> labels.any(label::contains) }
        ?.second
}

/** 判断 WCDB 插入是否属于已知微信支付消息结构。 */
private fun isSupportedMessageRecord(record: WechatMessageRecord): Boolean = when {
    record.table.equals("message", ignoreCase = true) -> record.type in WECHAT_MESSAGE_TYPES
    record.table.equals("AppMessage", ignoreCase = true) -> record.type in WECHAT_APP_MESSAGE_TYPES
    else -> false
}

/** 从有限 XML 文本中读取首个指定标签值。 */
private fun findXmlValue(text: String, tags: List<String>): String? {
    tags.forEach { tag ->
        val openStart = text.indexOf("<$tag", ignoreCase = true)
        if (openStart < 0) return@forEach
        val valueStart = text.indexOf('>', openStart).takeIf { it >= 0 }?.plus(1) ?: return@forEach
        val closeStart = text.indexOf("</$tag>", valueStart, ignoreCase = true)
        if (closeStart < 0) return@forEach
        val value = text.substring(valueStart, closeStart)
            .removePrefix("<![CDATA[")
            .removeSuffix("]]>")
            .trim()
        if (value.isNotEmpty()) return value
    }
    return null
}

/** 去掉 XML 标签并保留字段之间的换行。 */
private fun stripMarkup(text: String): String {
    return Regex("<[^>]+>").replace(text.replace("<![CDATA[", "").replace("]]>", ""), "\n")
}

/** 从换行文本中读取标签同行或下一行的值。 */
private fun findLabeledValue(text: String, labels: List<String>): String? {
    val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
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

/** 把微信账单中的本地支付时间转换为毫秒时间戳。 */
private fun parseWechatTime(value: String?): Long? = value?.let { text ->
    runCatching {
        LocalDateTime.parse(text.trim(), WECHAT_TIME_FORMATTER)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

/** 在已解析的微信结构化响应中查找首个中文日期时间。 */
private fun findWechatTime(element: JsonElement): Long? = when (element) {
    is JsonPrimitive -> element.contentOrNull
        ?.let(WECHAT_TIME_PATTERN::find)
        ?.value
        ?.let(::parseWechatTime)
    is JsonObject -> element.values.firstNotNullOfOrNull(::findWechatTime)
    is JsonArray -> element.firstNotNullOfOrNull(::findWechatTime)
}

private const val XWEB_CALLBACK_MARKER = "WeixinJSBridge._handleMessageFromWeixin("
private const val XWEB_SUCCESS_MARKER = "nativeWXPayCgiTunnel:ok"
private val WECHAT_MESSAGE_TYPES = setOf(318767153L, 419430449L)
private val WECHAT_APP_MESSAGE_TYPES = setOf(5L, 2000L)
private val WECHAT_INCOME_KEYWORDS = listOf("收款成功", "收款到账", "退款成功", "退款到账", "已退款", "向你转账")
private val WECHAT_EXPENSE_KEYWORDS = listOf("支付成功", "付款成功", "扣款成功", "消费成功", "转账成功", "转账给")
private val WECHAT_MERCHANT_LABELS = listOf("商户", "收款方", "付款给", "收款人", "交易对象", "商品", "付款方")
private val WECHAT_PAYMENT_METHOD_LABELS = listOf("付款方式", "支付方式", "扣款方式", "付款账户", "入账方式")
private val WECHAT_EXTERNAL_ID_LABELS = listOf("交易单号", "订单号", "商户单号", "交易流水号", "转账单号")
private val WECHAT_TIME_LABELS = listOf("支付时间", "交易时间", "收款时间", "转账时间", "退款时间")
private val WECHAT_STATUS_LABELS = listOf("当前状态", "交易状态", "支付状态")
private val WECHAT_PRODUCT_LABELS = listOf("商品")
private val WECHAT_DETAIL_SUCCESS_KEYWORDS = listOf("支付成功", "付款成功", "收款成功", "交易成功", "已退款", "退款成功")
private val WECHAT_MERCHANT_XML_TAGS = listOf("merchant_name", "payee_name", "receiver_name", "payer_name")
private val WECHAT_PAYMENT_METHOD_XML_TAGS = listOf("payment_method", "pay_tool", "bank_name")
private val WECHAT_EXTERNAL_ID_XML_TAGS = listOf("transaction_id", "trans_id", "order_id", "transferid")
private val WECHAT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日 H:mm:ss", Locale.CHINA)
private val WECHAT_TIME_PATTERN = Regex("[0-9]{4}年[0-9]{1,2}月[0-9]{1,2}日\\s+[0-9]{1,2}:[0-9]{2}:[0-9]{2}")
private val WECHAT_DOM_AMOUNT_PATTERN = Regex(
    "(?m)^\\s*([+＋\\-−﹣])\\s*[¥￥]?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*$",
)
private val WECHAT_DOM_EXPENSE_SIGNS = setOf("-", "−", "﹣")
