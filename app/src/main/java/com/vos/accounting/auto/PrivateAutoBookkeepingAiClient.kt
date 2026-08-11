package com.vos.accounting.auto

import android.graphics.Bitmap
import android.util.Base64
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.AppSettingsEntity
import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.MAX_AMOUNT_MINOR
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** 表示私有 AI 严格允许返回的有限账单字段。 */
@Serializable
private data class AiBookkeepingFields(
    val type: TransactionType,
    val amount: String,
    val currency: String = "cny",
    val merchant: String = "",
    @SerialName("payment_method")
    val paymentMethod: String = "",
    @SerialName("occurred_at")
    val occurredAt: Long? = null,
)

/** 调用用户配置的 OpenAI 兼容服务，并严格解析最小账单结构。 */
class PrivateAutoBookkeepingAiClient(
    private val repository: AccountingRepository,
    private val credentialStore: AutoAiCredentialStore,
) {
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    /** 在功能启用且端点合法时解析临时文本，不持久化请求或响应原文。 */
    suspend fun parse(
        provider: PaymentProvider,
        occurredAt: Long,
        textParts: List<String>,
        bitmap: Bitmap? = null,
    ): AutoBookkeepingCapture? = withContext(Dispatchers.IO) {
        val settings = repository.getSettingsSnapshot()
        if (!settings.autoCloudAiEnabled || settings.autoAiBaseUrl.isBlank() || settings.autoAiModel.isBlank()) {
            return@withContext null
        }
        val endpoint = resolveEndpoint(settings) ?: return@withContext null
        val redactedText = redactAiText(textParts.joinToString("\n"))
        if (redactedText.isBlank() && (bitmap == null || !settings.autoAiVisionEnabled)) return@withContext null
        val request = buildRequest(settings, redactedText, bitmap)
        val apiKey = credentialStore.read()
        val response = execute(endpoint, request, apiKey) ?: return@withContext null
        parseResponse(response, provider, occurredAt)
    }

    /** 只执行轻量连接与响应格式测试，不创建账单草稿。 */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        val settings = repository.getSettingsSnapshot()
        val endpoint = resolveEndpoint(settings) ?: return@withContext false
        val request = buildRequest(settings, "支付成功 ￥0.01，仅用于连接测试，不要执行真实支付。", null)
        val response = execute(endpoint, request, credentialStore.read()) ?: return@withContext false
        parseResponse(response, PaymentProvider.WECHAT, System.currentTimeMillis()) != null
    }

    /** 构造文本或可选视觉输入的 OpenAI 兼容请求。 */
    private fun buildRequest(settings: AppSettingsEntity, text: String, bitmap: Bitmap?): String {
        val content = if (bitmap != null && settings.autoAiVisionEnabled) {
            buildJsonArray {
                add(buildJsonObject { put("type", "text"); put("text", AI_PROMPT + text) })
                encodeBitmap(bitmap)?.let { dataUrl ->
                    add(
                        buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject { put("url", dataUrl) })
                        },
                    )
                }
            }
        } else {
            JsonPrimitive(AI_PROMPT + text)
        }
        return buildJsonObject {
            put("model", settings.autoAiModel)
            put("temperature", 0)
            put("response_format", buildJsonObject { put("type", "json_object") })
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", content)
                        },
                    )
                },
            )
        }.toString()
    }

    /** 发送一次有界 HTTP 请求，不绕过 TLS 证书校验。 */
    private fun execute(endpoint: URL, request: String, apiKey: String?): String? {
        val connection = endpoint.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = AI_CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = AI_READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!apiKey.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.outputStream.use { output -> output.write(request.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { reader -> reader.readText().take(MAX_AI_RESPONSE_CHARS) }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** 从兼容响应中严格读取 JSON 对象并转换金额为 Long 最小单位。 */
    private fun parseResponse(
        response: String,
        provider: PaymentProvider,
        occurredAt: Long,
    ): AutoBookkeepingCapture? {
        val root = runCatching { json.parseToJsonElement(response).jsonObject }.getOrNull() ?: return null
        val choices = root["choices"] as? JsonArray ?: return null
        val choice = choices.firstOrNull() as? JsonObject ?: return null
        val message = choice["message"] as? JsonObject ?: return null
        val content = (message["content"] as? JsonPrimitive)?.contentOrNull ?: return null
        val fields = runCatching { json.decodeFromString(AiBookkeepingFields.serializer(), content) }.getOrNull()
            ?: return null
        val amountMinor = runCatching {
            BigDecimal(fields.amount.replace(",", "")).movePointRight(2).longValueExact()
        }.getOrNull()?.takeIf { it in 1..MAX_AMOUNT_MINOR } ?: return null
        val currency = fields.currency.lowercase().takeIf { it.matches(Regex("[a-z]{3}")) } ?: return null
        val provenance = buildMap {
            put("type", AutoCaptureSource.CLOUD_AI)
            put("amount", AutoCaptureSource.CLOUD_AI)
            if (fields.merchant.isNotBlank()) put("merchant", AutoCaptureSource.CLOUD_AI)
            if (fields.paymentMethod.isNotBlank()) put("payment_method", AutoCaptureSource.CLOUD_AI)
        }
        return AutoBookkeepingCapture(
            provider = provider,
            source = AutoCaptureSource.CLOUD_AI,
            type = fields.type,
            amountMinor = amountMinor,
            currencyKey = currency,
            merchant = fields.merchant.trim(),
            occurredAt = fields.occurredAt?.takeIf { it > 0 } ?: occurredAt,
            paymentMethodKey = fields.paymentMethod.trim(),
            fieldProvenance = provenance,
            aiAssisted = true,
        )
    }

    /** 根据 HTTPS 与私网 HTTP 策略生成固定 chat/completions 端点。 */
    private fun resolveEndpoint(settings: AppSettingsEntity): URL? = runCatching {
        val base = URI(settings.autoAiBaseUrl.trim())
        require(base.userInfo == null && base.fragment == null && base.query == null)
        when (base.scheme?.lowercase()) {
            "https" -> Unit
            "http" -> require(settings.autoAiAllowInsecureLanHttp && isPrivateHost(base.host))
            else -> error("不支持的协议")
        }
        val path = base.path.trimEnd('/') + "/chat/completions"
        URI(base.scheme, null, base.host, base.port, path, null, null).toURL()
    }.getOrNull()

    /** 判断主机解析结果是否全部属于回环或私有地址。 */
    private fun isPrivateHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val addresses = runCatching { InetAddress.getAllByName(host).toList() }.getOrNull() ?: return false
        return addresses.isNotEmpty() && addresses.all { address ->
            val bytes = address.address
            address.isLoopbackAddress || address.isSiteLocalAddress || address.isAnyLocalAddress ||
                (bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC)
        }
    }

    /** 把视觉截图压缩为一次性内存 data URL。 */
    private fun encodeBitmap(bitmap: Bitmap): String? = runCatching {
        val bytes = ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, AI_IMAGE_JPEG_QUALITY, output))
            output.toByteArray()
        }
        require(bytes.size <= MAX_AI_IMAGE_BYTES)
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()
}

/** 删除订单号、卡号、手机号和其他长数字标识，并限制文本体积低于 8KiB。 */
internal fun redactAiText(text: String): String = text
    .replace(Regex("(?<!\\d)1[3-9]\\d{9}(?!\\d)"), "[手机号]")
    .replace(Regex("(?i)(订单号|交易号|流水号|商户单号)\\s*[:：]?\\s*[A-Za-z0-9_-]{6,}"), "$1：[已移除]")
    .replace(Regex("(?<!\\d)\\d{6,}(?!\\d)"), "[长数字]")
    .take(MAX_AI_TEXT_CHARS)

private const val AI_CONNECT_TIMEOUT_MILLIS = 5_000
private const val AI_READ_TIMEOUT_MILLIS = 25_000
private const val MAX_AI_TEXT_CHARS = 2_700
private const val MAX_AI_RESPONSE_CHARS = 16_384
private const val MAX_AI_IMAGE_BYTES = 512 * 1024
private const val AI_IMAGE_JPEG_QUALITY = 72
private const val AI_PROMPT =
    "仅从支付页文本提取账单。只返回 JSON：type(EXPENSE/INCOME)、amount(十进制元)、currency(三字母小写)、merchant、payment_method、occurred_at(毫秒时间戳，可省略)。不要推测缺失金额。文本：\n"
