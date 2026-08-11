package com.vos.accounting.hook

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import com.vos.accounting.auto.HOOK_CAPTURE_BINDER_DESCRIPTOR
import com.vos.accounting.auto.HOOK_CAPTURE_TRANSACTION
import com.vos.accounting.auto.HOOK_STATUS_ACTIVE
import com.vos.accounting.auto.HookCapturePayload
import com.vos.accounting.auto.HookPayloadKind
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.ArrayDeque
import kotlinx.serialization.json.Json

/** 在微信与支付宝进程中安装 API 102 文本结果页 Hook。 */
class AutoBookkeepingXposedEntry : XposedModule() {
    private val json = Json { encodeDefaults = true; explicitNulls = false }
    private var loadedProcess = ""
    private var applicationHookInstalled = false

    /** 记录当前模块实例所在进程和框架版本。 */
    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        loadedProcess = param.processName
        log(
            Log.INFO,
            HOOK_LOG_TAG,
            "event=module_loaded process=${param.processName} api=${getApiVersion()} " +
                "framework=${getFrameworkName()} ${getFrameworkVersion()}",
        )
    }

    /** 只在微信或支付宝自身进程的目标 ClassLoader 就绪后安装 Hook。 */
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        val provider = when (param.packageName) {
            WECHAT_PACKAGE_NAME -> PaymentProvider.WECHAT
            ALIPAY_PACKAGE_NAME -> PaymentProvider.ALIPAY
            else -> return
        }
        if (loadedProcess != param.packageName && !loadedProcess.startsWith("${param.packageName}:")) return
        if (applicationHookInstalled) return
        val adapterId = adapterId(provider) ?: return
        val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        try {
            hook(attach)
                .setId(APPLICATION_ATTACH_HOOK_ID)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    val result = chain.proceed()
                    val context = chain.getArg(0) as? Context ?: return@intercept result
                    onApplicationAttached(context, provider, adapterId)
                    result
                }
            applicationHookInstalled = true
            log(
                Log.INFO,
                HOOK_LOG_TAG,
                "event=hook_registered package=${param.packageName} process=$loadedProcess " +
                    "hook=$APPLICATION_ATTACH_HOOK_ID",
            )
        } catch (error: Throwable) {
            log(
                Log.ERROR,
                HOOK_LOG_TAG,
                "event=hook_register_failed package=${param.packageName} process=$loadedProcess",
                error,
            )
        }
    }

    /** 在支付应用 Context 可用后提交心跳并安装文本收集 Hook。 */
    private fun onApplicationAttached(
        context: Context,
        provider: PaymentProvider,
        adapterId: String,
    ) {
        val version = runCatching {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            ).versionName.orEmpty()
        }.getOrDefault("")
        submitHookCapture(
            context,
            HookCapturePayload(
                kind = HookPayloadKind.HEARTBEAT,
                provider = provider,
                packageName = context.packageName,
                appVersion = version,
                adapterId = adapterId,
                adapterStatus = HOOK_STATUS_ACTIVE,
            ),
        )
        installTextResultAdapter(context, provider, version, adapterId)
        log(
            Log.INFO,
            HOOK_LOG_TAG,
            "event=adapter_ready package=${context.packageName} process=$loadedProcess " +
                "version=$version adapter=$adapterId",
        )
    }

    /** Hook 所有首参数为文本的 TextView.setText 重载。 */
    private fun installTextResultAdapter(
        context: Context,
        provider: PaymentProvider,
        version: String,
        adapterId: String,
    ) {
        val collector = HookTextCollector(context, provider, version, adapterId) { payload ->
            submitHookCapture(context, payload)
        }
        val methods = TextView::class.java.declaredMethods.filter { method ->
            method.name == "setText" &&
                method.parameterTypes.firstOrNull()?.let(CharSequence::class.java::isAssignableFrom) == true
        }
        methods.forEach { method ->
            hook(method)
                .setId(TEXT_VIEW_SET_TEXT_HOOK_ID)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    (chain.getArg(0) as? CharSequence)?.let { collector.offer(it.toString()) }
                    chain.proceed()
                }
        }
        log(
            Log.INFO,
            HOOK_LOG_TAG,
            "event=text_hooks_registered process=$loadedProcess count=${methods.size}",
        )
    }

    /** 通过显式 Binder 把一条有限 JSON 载荷提交给主应用。 */
    private fun submitHookCapture(context: Context, payload: HookCapturePayload) {
        val payloadText = json.encodeToString(HookCapturePayload.serializer(), payload)
        if (payloadText.toByteArray(Charsets.UTF_8).size > MAX_HOOK_PAYLOAD_BYTES) return
        val intent = Intent().setComponent(
            ComponentName(ACCOUNTING_PACKAGE_NAME, HOOK_CAPTURE_SERVICE_NAME),
        )
        val connection = object : ServiceConnection {
            /** Binder 可用后完成一次同步小载荷事务并立即解绑。 */
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(HOOK_CAPTURE_BINDER_DESCRIPTOR)
                    data.writeString(payloadText)
                    checkNotNull(service).transact(HOOK_CAPTURE_TRANSACTION, data, reply, 0)
                    reply.readException()
                    log(
                        Log.INFO,
                        HOOK_LOG_TAG,
                        "event=capture_service_result accepted=${reply.readInt() == 1}",
                    )
                } catch (error: Exception) {
                    log(Log.WARN, HOOK_LOG_TAG, "event=capture_service_transaction_failed", error)
                } finally {
                    data.recycle()
                    reply.recycle()
                    runCatching { context.unbindService(this) }
                }
            }

            /** 记录宿主服务异常断开，不重试原始载荷。 */
            override fun onServiceDisconnected(name: ComponentName?) {
                log(Log.WARN, HOOK_LOG_TAG, "event=capture_service_disconnected")
            }
        }
        val bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (error: Exception) {
            log(Log.WARN, HOOK_LOG_TAG, "event=capture_service_bind_failed", error)
            false
        }
        if (!bound) log(Log.WARN, HOOK_LOG_TAG, "event=capture_service_bind_rejected")
    }
}

/** 返回与应用版本无关的受限文本收集器标识。 */
private fun adapterId(provider: PaymentProvider): String? = when (provider) {
    PaymentProvider.WECHAT -> "wechat_text_result_v1"
    PaymentProvider.ALIPAY -> "alipay_text_result_v1"
    PaymentProvider.UNIONPAY -> null
}

/** 在内存中短暂聚合结果页 TextView，并提交一次结构化账单。 */
private class HookTextCollector(
    private val context: Context,
    private val provider: PaymentProvider,
    private val version: String,
    private val adapterId: String,
    private val submit: (HookCapturePayload) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val entries = ArrayDeque<TimedText>()
    private var pendingSubmit: Runnable? = null
    private var lastFingerprint = ""
    private var lastSubmittedAt = 0L

    /** 接收短文本并在结果关键词出现后等待页面稳定。 */
    fun offer(rawText: String) {
        val text = rawText.trim().takeIf { it.isNotEmpty() && it.length <= MAX_HOOK_TEXT_LENGTH } ?: return
        val now = SystemClock.elapsedRealtime()
        while (entries.isNotEmpty() && now - entries.first().recordedAt > HOOK_TEXT_WINDOW_MILLIS) {
            entries.removeFirst()
        }
        if (entries.none { it.text == text }) entries.addLast(TimedText(text, now))
        val joined = entries.joinToString(" ") { it.text }
        if (RESULT_KEYWORDS.none(joined::contains)) return
        pendingSubmit?.let(handler::removeCallbacks)
        pendingSubmit = Runnable(::submitStableResult).also { handler.postDelayed(it, HOOK_SETTLE_MILLIS) }
    }

    /** 从稳定文本生成有限结构并执行十秒内存去重。 */
    private fun submitStableResult() {
        val lines = entries.map(TimedText::text)
        val joined = lines.joinToString(" ")
        val type = detectHookType(joined) ?: return
        val amount = findHookAmount(joined) ?: return
        val fingerprint = sha256Hook("$type|$amount|$joined")
        val now = SystemClock.elapsedRealtime()
        if (fingerprint == lastFingerprint && now - lastSubmittedAt < HOOK_SUBMIT_TTL_MILLIS) return
        lastFingerprint = fingerprint
        lastSubmittedAt = now
        submit(
            HookCapturePayload(
                kind = HookPayloadKind.CAPTURE,
                provider = provider,
                packageName = context.packageName,
                appVersion = version,
                adapterId = adapterId,
                type = type,
                amount = amount,
                merchant = findHookLabeledValue(lines, MERCHANT_LABELS).orEmpty(),
                paymentMethod = findHookLabeledValue(lines, PAYMENT_METHOD_LABELS).orEmpty(),
                externalTransactionId = findHookLabeledValue(lines, EXTERNAL_ID_LABELS),
                occurredAt = System.currentTimeMillis(),
            ),
        )
    }

    /** 表示一条仅在支付进程内存中短暂存在的页面文本。 */
    private data class TimedText(val text: String, val recordedAt: Long)
}

/** 从 Hook 结果文本判断收入或支出方向。 */
private fun detectHookType(text: String): TransactionType? = when {
    INCOME_KEYWORDS.any(text::contains) -> TransactionType.INCOME
    EXPENSE_KEYWORDS.any(text::contains) -> TransactionType.EXPENSE
    else -> null
}

/** 返回 Hook 文本中的首个合法十进制金额字符串。 */
private fun findHookAmount(text: String): String? = HOOK_AMOUNT_PATTERNS.firstNotNullOfOrNull { pattern ->
    pattern.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.let { value ->
        runCatching { BigDecimal(value).setScale(2).toPlainString() }.getOrNull()
    }
}

/** 读取 Hook 文本中的同节点或相邻节点标签值。 */
private fun findHookLabeledValue(lines: List<String>, labels: List<String>): String? {
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

/** 生成 Hook 进程内十秒去重使用的摘要。 */
private fun sha256Hook(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }

private const val ACCOUNTING_PACKAGE_NAME = "com.vos.accounting"
private const val HOOK_CAPTURE_SERVICE_NAME = "com.vos.accounting.auto.HookCaptureService"
private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
private const val ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone"
private const val HOOK_LOG_TAG = "AccountingHook"
private const val APPLICATION_ATTACH_HOOK_ID = "application_attach"
private const val TEXT_VIEW_SET_TEXT_HOOK_ID = "text_view_set_text"
private const val MAX_HOOK_PAYLOAD_BYTES = 64 * 1024
private const val MAX_HOOK_TEXT_LENGTH = 256
private const val HOOK_TEXT_WINDOW_MILLIS = 4_000L
private const val HOOK_SETTLE_MILLIS = 650L
private const val HOOK_SUBMIT_TTL_MILLIS = 10_000L
private val RESULT_KEYWORDS = listOf("支付成功", "付款成功", "收款成功", "退款成功", "转账成功", "扣款成功")
private val INCOME_KEYWORDS = listOf("收款成功", "收款到账", "退款成功", "退款到账", "已退款")
private val EXPENSE_KEYWORDS = listOf("支付成功", "付款成功", "扣款成功", "消费成功", "转账成功")
private val HOOK_AMOUNT_PATTERNS = listOf(
    Regex("(?:¥|￥|人民币|CNY\\s*)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
    Regex("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*元"),
)
private val MERCHANT_LABELS = listOf("商户", "收款方", "付款给", "收款人", "交易对象", "商品")
private val PAYMENT_METHOD_LABELS = listOf("付款方式", "支付方式", "扣款方式", "付款账户", "转出账户")
private val EXTERNAL_ID_LABELS = listOf("交易单号", "订单号", "商户单号", "交易流水号")
