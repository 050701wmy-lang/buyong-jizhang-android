package com.vos.accounting.hook

import android.app.Application
import android.content.ComponentName
import android.content.ContentValues
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
import android.view.MotionEvent
import android.view.View
import android.webkit.ValueCallback
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
import java.util.WeakHashMap
import kotlinx.serialization.json.Json

/** 在微信与支付宝进程中安装 API 102 文本结果页 Hook。 */
class AutoBookkeepingXposedEntry : XposedModule() {
    private val json = Json { encodeDefaults = true; explicitNulls = false }
    private val wechatParser = WechatHookParser()
    private val wechatKindaCache = WechatKindaFieldCache()
    private val wechatDomHandler by lazy { Handler(Looper.getMainLooper()) }
    private val pendingWechatDomProbes = mutableMapOf<Any, Runnable>()
    private val observedWechatWebViews = WeakHashMap<Any, Boolean>()
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
        val targetClassLoader = param.classLoader
        val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        try {
            hook(attach)
                .setId(APPLICATION_ATTACH_HOOK_ID)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    val result = chain.proceed()
                    val context = chain.getArg(0) as? Context ?: return@intercept result
                    onApplicationAttached(context, provider, adapterId, targetClassLoader)
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
        targetClassLoader: ClassLoader,
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
        if (provider == PaymentProvider.WECHAT) {
            installWechatXWebAdapter(context, targetClassLoader, version)
            installWechatWcdbAdapter(context, targetClassLoader, version)
            installWechatKindaCache(targetClassLoader)
        }
        installTextResultAdapter(context, provider, version, adapterId)
        log(
            Log.INFO,
            HOOK_LOG_TAG,
            "event=adapter_ready package=${context.packageName} process=$loadedProcess " +
                "version=$version adapter=$adapterId",
        )
    }

    /** Hook 微信 XWeb 支付回调并提交已解析的结构化账单。 */
    private fun installWechatXWebAdapter(
        context: Context,
        targetClassLoader: ClassLoader,
        version: String,
    ) {
        try {
            val webViewClass = Class.forName(WECHAT_XWEB_CLASS_NAME, false, targetClassLoader)
            val method = webViewClass.getDeclaredMethod(
                "evaluateJavascript",
                String::class.java,
                ValueCallback::class.java,
            )
            hook(method)
                .setId(WECHAT_XWEB_HOOK_ID)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    val result = chain.proceed()
                    val script = chain.getArg(0) as? String
                    val capture = script?.let { value ->
                        runCatching { wechatParser.parseXWeb(value, wechatKindaCache.snapshot()) }.getOrNull()
                    }
                    if (capture != null) {
                        submitWechatCapture(context, version, WECHAT_XWEB_ADAPTER_ID, capture)
                        log(Log.INFO, HOOK_LOG_TAG, "event=hook_hit process=$loadedProcess channel=xweb")
                    }
                    result
                }
            installWechatXWebPageProbe(context, webViewClass, method, version)
            installWechatXWebTouchProbe(context, webViewClass, method, version)
            log(Log.INFO, HOOK_LOG_TAG, "event=hook_registered process=$loadedProcess channel=xweb")
        } catch (error: Throwable) {
            logHookUnavailable("xweb", error)
        }
    }

    /** Hook XWeb 客户端设置入口，并按实际客户端类安装页面稳定回调。 */
    private fun installWechatXWebPageProbe(
        context: Context,
        webViewClass: Class<*>,
        evaluateJavascript: java.lang.reflect.Method,
        version: String,
    ) {
        val setter = webViewClass.methods.firstOrNull { method ->
            method.name == "setWebViewClient" && method.parameterCount == 1
        } ?: run {
            log(Log.WARN, HOOK_LOG_TAG, "event=hook_unavailable process=$loadedProcess channel=xweb_page")
            return
        }
        hook(setter)
            .setId(WECHAT_XWEB_CLIENT_HOOK_ID)
            .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
            .intercept { chain ->
                val result = chain.proceed()
                val webView = chain.getThisObject()
                if (webView != null && webViewClass.isInstance(webView)) {
                    installWechatXWebDrawProbe(context, webView, evaluateJavascript, version)
                }
                result
            }
        log(Log.INFO, HOOK_LOG_TAG, "event=hook_registered process=$loadedProcess channel=xweb_page_client")
    }

    /** 监听实际 XWeb 的页面绘制，并在内容变化后触发稳定检查。 */
    private fun installWechatXWebDrawProbe(
        context: Context,
        webView: Any,
        evaluateJavascript: java.lang.reflect.Method,
        version: String,
    ) {
        val view = webView as? View ?: return
        if (observedWechatWebViews.put(webView, true) != null) return
        view.viewTreeObserver.addOnDrawListener {
            scheduleWechatXWebDomProbe(context, webView, evaluateJavascript, version)
        }
        log(Log.INFO, HOOK_LOG_TAG, "event=hook_registered process=$loadedProcess channel=xweb_draw")
    }

    /** 在用户点击 XWeb 后读取一次内存 DOM，以完整详情字段识别账单。 */
    private fun installWechatXWebTouchProbe(
        context: Context,
        webViewClass: Class<*>,
        evaluateJavascript: java.lang.reflect.Method,
        version: String,
    ) {
        val touchMethod = generateSequence(webViewClass) { it.superclass }
            .firstNotNullOfOrNull { owner ->
                WECHAT_XWEB_TOUCH_METHODS.firstNotNullOfOrNull { name ->
                    owner.declaredMethods.firstOrNull { method ->
                        method.name == name &&
                            method.parameterTypes.contentEquals(arrayOf(MotionEvent::class.java))
                    }
                }
            } ?: run {
            log(Log.WARN, HOOK_LOG_TAG, "event=hook_unavailable process=$loadedProcess channel=xweb_touch")
            return
        }
        hook(touchMethod)
            .setId(WECHAT_XWEB_TOUCH_HOOK_ID)
            .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
            .intercept { chain ->
                val result = chain.proceed()
                if (!webViewClass.isInstance(chain.getThisObject())) return@intercept result
                val action = (chain.getArg(0) as? MotionEvent)?.actionMasked
                val webView = chain.getThisObject()
                if (action == MotionEvent.ACTION_UP && webView != null) {
                    scheduleWechatXWebDomProbe(context, webView, evaluateJavascript, version)
                }
                result
            }
        log(
            Log.INFO,
            HOOK_LOG_TAG,
            "event=hook_registered process=$loadedProcess channel=xweb_touch method=${touchMethod.name}",
        )
    }

    /** 合并同一 XWeb 的连续信号，并在页面静止后启动 DOM 稳定检查。 */
    private fun scheduleWechatXWebDomProbe(
        context: Context,
        webView: Any,
        evaluateJavascript: java.lang.reflect.Method,
        version: String,
    ) {
        wechatDomHandler.post {
            if (pendingWechatDomProbes.containsKey(webView)) return@post
            val probe = Runnable {
                pendingWechatDomProbes.remove(webView)
                probeWechatXWebDom(context, webView, evaluateJavascript, version)
            }
            pendingWechatDomProbes[webView] = probe
            wechatDomHandler.postDelayed(probe, WECHAT_DOM_DEBOUNCE_MILLIS)
        }
    }

    /** 读取当前 XWeb 的可见文字，并只在严格匹配详情页时提交候选账单。 */
    private fun probeWechatXWebDom(
        context: Context,
        webView: Any,
        evaluateJavascript: java.lang.reflect.Method,
        version: String,
    ) {
        readWechatXWebDom(webView, evaluateJavascript) { firstText ->
            wechatDomHandler.postDelayed({
                readWechatXWebDom(webView, evaluateJavascript) { secondText ->
                    if (firstText != secondText) return@readWechatXWebDom
                    val capture = runCatching { wechatParser.parseXWebDom(secondText) }.getOrNull()
                        ?: return@readWechatXWebDom
                    submitWechatCapture(context, version, WECHAT_XWEB_DOM_ADAPTER_ID, capture)
                    log(Log.INFO, HOOK_LOG_TAG, "event=hook_hit process=$loadedProcess channel=xweb_dom")
                }
            }, WECHAT_DOM_STABILITY_MILLIS)
        }
    }

    /** 从当前 XWeb 读取一次内存 DOM 文本并交给调用方，不持久化原文。 */
    private fun readWechatXWebDom(
        webView: Any,
        evaluateJavascript: java.lang.reflect.Method,
        onText: (String) -> Unit,
    ) {
        val callback = ValueCallback<String> { encodedText -> encodedText?.let(onText) }
        runCatching {
            evaluateJavascript.invoke(webView, WECHAT_DOM_TEXT_SCRIPT, callback)
        }.onFailure { error ->
            log(Log.WARN, HOOK_LOG_TAG, "event=hook_failed process=$loadedProcess channel=xweb_dom", error)
        }
    }

    /** Hook 微信 WCDB 插入并仅检查支付消息需要的有限字段。 */
    private fun installWechatWcdbAdapter(
        context: Context,
        targetClassLoader: ClassLoader,
        version: String,
    ) {
        try {
            val databaseClass = Class.forName(WECHAT_WCDB_CLASS_NAME, false, targetClassLoader)
            val method = databaseClass.getDeclaredMethod(
                "insertWithOnConflict",
                String::class.java,
                String::class.java,
                ContentValues::class.java,
                Int::class.javaPrimitiveType,
            )
            hook(method)
                .setId(WECHAT_WCDB_HOOK_ID)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    val result = chain.proceed()
                    val table = chain.getArg(0) as? String
                    val values = chain.getArg(2) as? ContentValues
                    val record = if (table != null && values != null) {
                        readWechatMessageRecord(table, values)
                    } else {
                        null
                    }
                    val capture = record?.let { value ->
                        runCatching { wechatParser.parseMessage(value, wechatKindaCache.snapshot()) }.getOrNull()
                    }
                    if (capture != null) {
                        submitWechatCapture(context, version, WECHAT_WCDB_ADAPTER_ID, capture)
                        log(Log.INFO, HOOK_LOG_TAG, "event=hook_hit process=$loadedProcess channel=wcdb")
                    }
                    result
                }
            log(Log.INFO, HOOK_LOG_TAG, "event=hook_registered process=$loadedProcess channel=wcdb")
        } catch (error: Throwable) {
            logHookUnavailable("wcdb", error)
        }
    }

    /** Hook Kinda 富文本追加，将金额、商户和付款方式在内存中保留两分钟。 */
    private fun installWechatKindaCache(targetClassLoader: ClassLoader) {
        try {
            val richTextClass = Class.forName(WECHAT_KINDA_CLASS_NAME, false, targetClassLoader)
            val method = richTextClass.getDeclaredMethod("appendText", String::class.java)
            hook(method)
                .setId(WECHAT_KINDA_HOOK_ID)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    val result = chain.proceed()
                    (chain.getArg(0) as? String)?.let(wechatKindaCache::offer)
                    result
                }
            log(Log.INFO, HOOK_LOG_TAG, "event=hook_registered process=$loadedProcess channel=kinda")
        } catch (error: Throwable) {
            logHookUnavailable("kinda", error)
        }
    }

    /** 把微信解析结果转换为现有 Binder 载荷。 */
    private fun submitWechatCapture(
        context: Context,
        version: String,
        adapterId: String,
        capture: WechatParsedCapture,
    ) {
        submitHookCapture(
            context,
            HookCapturePayload(
                kind = HookPayloadKind.CAPTURE,
                provider = PaymentProvider.WECHAT,
                packageName = context.packageName,
                appVersion = version,
                adapterId = adapterId,
                type = capture.type,
                amount = capture.amount,
                merchant = capture.merchant,
                note = capture.note,
                paymentMethod = capture.paymentMethod,
                externalTransactionId = capture.externalTransactionId,
                occurredAt = capture.occurredAt,
            ),
        )
    }

    /** 记录某个微信结构化 Hook 不可用，不中断其他采集通道。 */
    private fun logHookUnavailable(channel: String, error: Throwable) {
        log(
            Log.WARN,
            HOOK_LOG_TAG,
            "event=hook_unavailable process=$loadedProcess channel=$channel reason=${error.javaClass.simpleName}",
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

/** 从 WCDB ContentValues 中复制解析账单所需的有限字段。 */
private fun readWechatMessageRecord(table: String, values: ContentValues): WechatMessageRecord {
    val fields = WECHAT_MESSAGE_FIELD_NAMES.mapNotNull { name ->
        values.getAsString(name)
            ?.take(MAX_WECHAT_MESSAGE_FIELD_LENGTH)
            ?.takeIf(String::isNotEmpty)
            ?.let { value -> name to value }
    }.toMap()
    return WechatMessageRecord(
        table = table,
        type = values.getAsLong("type"),
        isSend = values.getAsInteger("isSend") ?: values.getAsInteger("is_send"),
        createTime = normalizeWechatTimestamp(
            values.getAsLong("createTime") ?: values.getAsLong("create_time"),
        ),
        fields = fields,
    )
}

/** 把微信秒级或毫秒级时间戳统一成毫秒。 */
private fun normalizeWechatTimestamp(value: Long?): Long? = value?.let { timestamp ->
    if (timestamp in 1 until MIN_MILLISECOND_TIMESTAMP) timestamp * 1_000L else timestamp
}

/** 在微信进程内存中按字段分别缓存 Kinda 提供的有限值。 */
private class WechatKindaFieldCache {
    private var fields = WechatCachedFields()
    private var expiresAt = 0L

    /** 接收一段 Kinda 文本，只留下可明确判断的金额、商户或付款方式。 */
    @Synchronized
    fun offer(rawText: String) {
        val text = rawText.trim().takeIf {
            it.isNotEmpty() && it.length <= MAX_HOOK_TEXT_LENGTH
        } ?: return
        val now = SystemClock.elapsedRealtime()
        if (now > expiresAt) fields = WechatCachedFields()
        val updated = fields.copy(
            amount = findHookAmount(text) ?: fields.amount,
            merchant = findKindaMerchant(text) ?: fields.merchant,
            paymentMethod = text.takeIf { WECHAT_PAYMENT_METHOD_HINTS.any(it::contains) }
                ?: fields.paymentMethod,
        )
        if (updated == fields) return
        fields = updated
        expiresAt = now + WECHAT_KINDA_CACHE_MILLIS
    }

    /** 返回仍在两分钟有效期内的 Kinda 字段，并清除过期值。 */
    @Synchronized
    fun snapshot(): WechatCachedFields {
        if (SystemClock.elapsedRealtime() > expiresAt) fields = WechatCachedFields()
        return fields
    }
}

/** 从 Kinda 短文本中提取带明确前缀的交易对象。 */
private fun findKindaMerchant(text: String): String? {
    WECHAT_KINDA_MERCHANT_PREFIXES.forEach { prefix ->
        val value = text.substringAfter(prefix, "")
            .trim('：', ':', ' ', '，', ',')
            .takeIf { text.contains(prefix) && it.isNotEmpty() && it.length <= MAX_WECHAT_MERCHANT_LENGTH }
        if (value != null) return value
    }
    return null
}

/** 返回与应用版本无关的受限文本收集器标识。 */
private fun adapterId(provider: PaymentProvider): String? = when (provider) {
    PaymentProvider.WECHAT -> "wechat_multi_source_v2"
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

/** 把带符号的 Hook 金额规范为正数两位小数。 */
internal fun normalizeHookAmount(value: String?): String? = value
    ?.trim()
    ?.removePrefix("¥")
    ?.removePrefix("￥")
    ?.replace(",", "")
    ?.takeIf(String::isNotEmpty)
    ?.let { raw -> runCatching { BigDecimal(raw).abs().setScale(2).toPlainString() }.getOrNull() }

/** 返回 Hook 文本中的首个合法十进制金额字符串。 */
internal fun findHookAmount(text: String): String? = HOOK_AMOUNT_PATTERNS.firstNotNullOfOrNull { pattern ->
    pattern.find(text)?.groupValues?.getOrNull(1)?.let(::normalizeHookAmount)
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
private const val WECHAT_XWEB_CLASS_NAME = "com.tencent.xweb.WebView"
private const val WECHAT_WCDB_CLASS_NAME = "com.tencent.wcdb.database.SQLiteDatabase"
private const val WECHAT_KINDA_CLASS_NAME = "com.tencent.kinda.framework.widget.base.MMKRichText"
private const val HOOK_LOG_TAG = "AccountingHook"
private const val APPLICATION_ATTACH_HOOK_ID = "application_attach"
private const val TEXT_VIEW_SET_TEXT_HOOK_ID = "text_view_set_text"
private const val WECHAT_XWEB_HOOK_ID = "wechat_xweb_evaluate_javascript"
private const val WECHAT_XWEB_CLIENT_HOOK_ID = "wechat_xweb_client"
private const val WECHAT_XWEB_TOUCH_HOOK_ID = "wechat_xweb_touch"
private const val WECHAT_WCDB_HOOK_ID = "wechat_wcdb_insert"
private const val WECHAT_KINDA_HOOK_ID = "wechat_kinda_append_text"
private const val WECHAT_XWEB_ADAPTER_ID = "wechat_xweb_v1"
private const val WECHAT_XWEB_DOM_ADAPTER_ID = "wechat_xweb_dom_v1"
private const val WECHAT_WCDB_ADAPTER_ID = "wechat_wcdb_v1"
private const val MAX_HOOK_PAYLOAD_BYTES = 64 * 1024
private const val MAX_HOOK_TEXT_LENGTH = 256
private const val MAX_WECHAT_MESSAGE_FIELD_LENGTH = 16 * 1024
private const val MAX_WECHAT_MERCHANT_LENGTH = 80
private const val MIN_MILLISECOND_TIMESTAMP = 1_000_000_000_000L
private const val HOOK_TEXT_WINDOW_MILLIS = 4_000L
private const val HOOK_SETTLE_MILLIS = 650L
private const val HOOK_SUBMIT_TTL_MILLIS = 10_000L
private const val WECHAT_KINDA_CACHE_MILLIS = 2 * 60 * 1_000L
private const val WECHAT_DOM_DEBOUNCE_MILLIS = 500L
private const val WECHAT_DOM_STABILITY_MILLIS = 250L
private const val WECHAT_DOM_TEXT_SCRIPT = "(function(){return document.body?document.body.innerText:'';})()"
private val WECHAT_XWEB_TOUCH_METHODS = setOf("onTouchEvent", "dispatchTouchEvent")
private val WECHAT_MESSAGE_FIELD_NAMES = listOf("content", "description", "title", "xml", "extinfo", "reserved")
private val WECHAT_PAYMENT_METHOD_HINTS = listOf("零钱", "零钱通", "银行卡", "信用卡", "储蓄卡", "经营账户")
private val WECHAT_KINDA_MERCHANT_PREFIXES = listOf("转账给", "付款给", "收款方", "商户", "收款人")
private val RESULT_KEYWORDS = listOf("支付成功", "付款成功", "收款成功", "退款成功", "转账成功", "扣款成功")
private val INCOME_KEYWORDS = listOf("收款成功", "收款到账", "退款成功", "退款到账", "已退款")
private val EXPENSE_KEYWORDS = listOf("支付成功", "付款成功", "扣款成功", "消费成功", "转账成功")
private val HOOK_AMOUNT_PATTERNS = listOf(
    Regex("(?:¥|￥|人民币|CNY\\s*)\\s*([+-]?[0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
    Regex("([+-]?[0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*元"),
)
private val MERCHANT_LABELS = listOf("商户", "收款方", "付款给", "收款人", "交易对象", "商品")
private val PAYMENT_METHOD_LABELS = listOf("付款方式", "支付方式", "扣款方式", "付款账户", "转出账户")
private val EXTERNAL_ID_LABELS = listOf("交易单号", "订单号", "商户单号", "交易流水号")
