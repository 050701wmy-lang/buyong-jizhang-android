package com.vos.accounting.auto

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.util.Log
import com.vos.accounting.AccountingApplication
import com.vos.accounting.data.AutoHookHeartbeatEntity
import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.MAX_AMOUNT_MINOR
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import java.math.BigDecimal
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val HOOK_CAPTURE_BINDER_DESCRIPTOR = "com.vos.accounting.auto.HookCaptureService"
const val HOOK_CAPTURE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION
const val MAX_HOOK_PAYLOAD_BYTES = 64 * 1024

/** 区分 Hook 模块装载心跳与结构化账单采集。 */
@Serializable
enum class HookPayloadKind {
    HEARTBEAT,
    CAPTURE,
}

/** 承载 Hook 进程提交的有限结构化数据。 */
@Serializable
data class HookCapturePayload(
    val kind: HookPayloadKind,
    val provider: PaymentProvider,
    @SerialName("package_name")
    val packageName: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("adapter_id")
    val adapterId: String,
    @SerialName("adapter_status")
    val adapterStatus: String = HOOK_STATUS_ACTIVE,
    val type: TransactionType? = null,
    val amount: String? = null,
    val currency: String = "cny",
    val merchant: String = "",
    val note: String = "",
    @SerialName("payment_method")
    val paymentMethod: String = "",
    @SerialName("external_transaction_id")
    val externalTransactionId: String? = null,
    @SerialName("occurred_at")
    val occurredAt: Long = 0,
)

/** 接收 LSPosed 注入进程的显式 Binder 请求并复用统一载荷处理器。 */
class HookCaptureService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var processor: HookCaptureProcessor

    private val binder = object : Binder() {
        /** 校验 Binder 调用者后异步接收一条有限载荷。 */
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != HOOK_CAPTURE_TRANSACTION) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(HOOK_CAPTURE_BINDER_DESCRIPTOR)
            val payload = processor.validate(Binder.getCallingUid(), data.readString().orEmpty())
            if (payload != null) serviceScope.launch { processor.process(payload) }
            reply?.writeNoException()
            reply?.writeInt(if (payload != null) 1 else 0)
            return true
        }
    }

    /** 初始化统一 Hook 载荷处理器。 */
    override fun onCreate() {
        super.onCreate()
        processor = HookCaptureProcessor(this)
    }

    /** 仅返回自定义 Binder，不通过 Intent extras 接收账单数据。 */
    override fun onBind(intent: Intent?): IBinder = binder

    /** 结束服务协程并丢弃尚未完成的异步操作。 */
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

/** 统一执行 Hook 载荷的 UID、限速、schema 校验与待确认采集。 */
private class HookCaptureProcessor(context: Context) {
    private val application = context.applicationContext as AccountingApplication
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    private val captureHandler = AutoBookkeepingCaptureHandler(context.applicationContext)

    /** 返回通过大小、速率、严格 JSON、平台与调用 UID 校验的载荷。 */
    fun validate(uid: Int, payloadText: String): HookCapturePayload? {
        if (uid == Process.INVALID_UID) return null
        if (payloadText.toByteArray(Charsets.UTF_8).size !in 1..MAX_HOOK_PAYLOAD_BYTES) return null
        if (!HookRateLimiter.acquire(uid)) return null
        val payload = runCatching {
            json.decodeFromString(HookCapturePayload.serializer(), payloadText)
        }.getOrNull() ?: return null
        val expectedPackage = packageForProvider(payload.provider) ?: return null
        if (payload.packageName != expectedPackage) return null
        val callerPackages = application.packageManager.getPackagesForUid(uid)?.toSet().orEmpty()
        if (expectedPackage !in callerPackages) return null
        if (payload.provider == PaymentProvider.UNIONPAY) return null
        return payload
    }

    /** 根据载荷类型记录心跳或进入唯一待确认账单入口。 */
    suspend fun process(payload: HookCapturePayload) {
        when (payload.kind) {
            HookPayloadKind.HEARTBEAT -> acceptHeartbeat(payload)
            HookPayloadKind.CAPTURE -> acceptCapture(payload)
        }
    }

    /** 记录已成功装载通用文本适配器的 Hook 心跳。 */
    private suspend fun acceptHeartbeat(payload: HookCapturePayload) {
        if (payload.adapterStatus != HOOK_STATUS_ACTIVE) return
        application.repository.upsertAutoHookHeartbeat(
            AutoHookHeartbeatEntity(
                provider = payload.provider,
                packageName = payload.packageName,
                appVersion = payload.appVersion.take(80),
                status = HOOK_STATUS_ACTIVE,
                lastLoadedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** 校验结构化账单并通过现有 PENDING 草稿入口保存。 */
    private suspend fun acceptCapture(payload: HookCapturePayload) {
        if (payload.adapterStatus != HOOK_STATUS_ACTIVE || payload.adapterId.length !in 1..100) return
        val type = payload.type ?: return
        val amountMinor = runCatching {
            BigDecimal(payload.amount?.replace(",", "") ?: return)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()?.takeIf { it in 1..MAX_AMOUNT_MINOR } ?: return
        val currency = payload.currency.lowercase().takeIf { it.matches(Regex("[a-z]{3}")) } ?: return
        val settings = application.repository.getSettingsSnapshot()
        if (!settings.autoXposedEnabled) return
        val now = System.currentTimeMillis()
        val capture = AutoBookkeepingCapture(
            provider = payload.provider,
            source = AutoCaptureSource.XPOSED,
            type = type,
            amountMinor = amountMinor,
            currencyKey = currency,
            merchant = payload.merchant.trim().take(200),
            note = payload.note.trim().take(200),
            occurredAt = payload.occurredAt.takeIf { it > 0 } ?: now,
            paymentMethodKey = payload.paymentMethod.trim().take(200),
            externalKeyHash = payload.externalTransactionId
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { hashExternalKey(payload.provider, it) },
            ruleId = payload.adapterId,
            rulePackVersion = 1,
            fieldProvenance = buildMap {
                put("type", AutoCaptureSource.XPOSED)
                put("amount", AutoCaptureSource.XPOSED)
                if (payload.merchant.isNotBlank()) put("merchant", AutoCaptureSource.XPOSED)
                if (payload.note.isNotBlank()) put("note", AutoCaptureSource.XPOSED)
                if (payload.paymentMethod.isNotBlank()) put("payment_method", AutoCaptureSource.XPOSED)
                if (!payload.externalTransactionId.isNullOrBlank()) put("external_key", AutoCaptureSource.XPOSED)
            },
        )
        captureHandler.handle(capture)
        application.repository.upsertAutoHookHeartbeat(
            AutoHookHeartbeatEntity(
                provider = payload.provider,
                packageName = payload.packageName,
                appVersion = payload.appVersion.take(80),
                status = HOOK_STATUS_ACTIVE,
                lastLoadedAt = now,
                lastCaptureAt = now,
            ),
        )
    }
}

/** 在进程内对每个调用 UID 限制一分钟内最多三十个请求。 */
private object HookRateLimiter {
    private val requestsByUid = mutableMapOf<Int, ArrayDeque<Long>>()

    /** 尝试占用一次 UID 请求额度。 */
    fun acquire(uid: Int): Boolean = synchronized(requestsByUid) {
        val now = System.currentTimeMillis()
        val requests = requestsByUid.getOrPut(uid) { ArrayDeque() }
        while (requests.isNotEmpty() && now - requests.first() > HOOK_RATE_WINDOW_MILLIS) requests.removeFirst()
        if (requests.size >= MAX_HOOK_REQUESTS_PER_MINUTE) return@synchronized false
        requests.addLast(now)
        true
    }
}

/** 返回 Hook 平台必须对应的真实支付应用包名。 */
private fun packageForProvider(provider: PaymentProvider): String? = when (provider) {
    PaymentProvider.WECHAT -> WECHAT_PACKAGE
    PaymentProvider.ALIPAY -> ALIPAY_PACKAGE
    PaymentProvider.UNIONPAY -> null
}

const val HOOK_STATUS_ACTIVE = "ACTIVE"
private const val HOOK_RATE_WINDOW_MILLIS = 60_000L
private const val MAX_HOOK_REQUESTS_PER_MINUTE = 30
