package com.vos.accounting.auto

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.AccountingApplication
import java.security.MessageDigest
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** 等待支付页面稳定后，仅提取声明式规则所需的临时节点文本。 */
class AutoBookkeepingAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingJobs = mutableMapOf<WindowKey, Job>()
    private val lastSuccessfulFingerprints = mutableMapOf<String, Long>()
    private lateinit var captureHandler: AutoBookkeepingCaptureHandler
    private lateinit var localOcr: LocalAutoBookkeepingOcr
    private lateinit var application: AccountingApplication

    /** 初始化共用采集处理器。 */
    override fun onCreate() {
        super.onCreate()
        application = applicationContext as AccountingApplication
        captureHandler = AutoBookkeepingCaptureHandler(this)
        localOcr = LocalAutoBookkeepingOcr(this)
    }

    /** 按包名和窗口重置 500ms 防抖，只让最后一次页面变化进入稳定性检查。 */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val accessibilityEvent = event ?: return
        val packageName = accessibilityEvent.packageName?.toString() ?: return
        if (paymentProviderForPackage(packageName) == null) return
        val key = WindowKey(packageName, accessibilityEvent.windowId)
        val activityName = accessibilityEvent.className?.toString()
        val eventText = buildList {
            accessibilityEvent.text.mapNotNullTo(this) { value ->
                value?.toString()?.trim()?.takeIf(String::isNotEmpty)
            }
            accessibilityEvent.contentDescription?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        }
        pendingJobs.remove(key)?.cancel()
        pendingJobs[key] = serviceScope.launch {
            delay(PAGE_DEBOUNCE_MILLIS)
            recognizeStablePage(key, activityName, eventText)
            pendingJobs.remove(key)
        }
    }

    /** 系统要求中断服务时取消尚未完成的稳定性读取。 */
    override fun onInterrupt() {
        pendingJobs.values.forEach(Job::cancel)
        pendingJobs.clear()
    }

    /** 结束服务协程，避免持有系统服务实例和临时页面内容。 */
    override fun onDestroy() {
        localOcr.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    /** 间隔 250ms 重读页面，结构和内容一致后才提交声明式规则。 */
    private suspend fun recognizeStablePage(
        key: WindowKey,
        activityName: String?,
        eventText: List<String>,
    ) {
        var previous = readSnapshot(eventText) ?: return
        repeat(MAX_STABILITY_RETRIES) {
            delay(STABILITY_READ_INTERVAL_MILLIS)
            val current = readSnapshot(eventText) ?: return
            if (previous.fingerprint == current.fingerprint) {
                if (recentlyHandled(current.fingerprint)) return
                val input = RuleInput(
                    packageName = key.packageName,
                    source = AutoCaptureSource.ACCESSIBILITY,
                    occurredAt = System.currentTimeMillis(),
                    textParts = current.textParts,
                    activityName = activityName,
                )
                val deterministic = withContext(Dispatchers.IO) { captureHandler.parseRules(input) }
                val handled = if (deterministic != null) {
                    withContext(Dispatchers.IO) { captureHandler.handleParsed(input, deterministic) }
                } else {
                    recognizeWithFallbacks(input)
                }
                if (handled) rememberSuccessful(current.fingerprint)
                return
            }
            previous = current
        }
    }

    /** 读取有限节点并同时生成内容与结构指纹，不保留节点对象。 */
    private fun readSnapshot(eventText: List<String>): PageSnapshot? {
        val root = rootInActiveWindow ?: return null
        val text = ArrayList<String>()
        val structure = StringBuilder()
        val nodes = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (nodes.isNotEmpty() && visited < MAX_ACCESSIBILITY_NODES) {
            val node = nodes.removeFirst()
            visited += 1
            if (node.isVisibleToUser) {
                val nodeText = node.text?.toString()?.trim().orEmpty()
                val description = node.contentDescription?.toString()?.trim().orEmpty()
                if (nodeText.isNotEmpty()) text += nodeText
                if (description.isNotEmpty()) text += description
                structure.append(node.className).append('|')
                    .append(node.viewIdResourceName).append('|')
                    .append(node.childCount).append('|')
                    .append(nodeText).append('|')
                    .append(description).append('\n')
            }
            repeat(node.childCount) { index -> node.getChild(index)?.let(nodes::addLast) }
        }
        val parts = (eventText + text).filter(String::isNotBlank).distinct()
        val fingerprint = sha256(structure.append("--text--\n").append(parts.joinToString("\n")).toString())
        return PageSnapshot(parts, fingerprint)
    }

    /** 判断相同成功页面是否仍在十秒内存去重窗口。 */
    private fun recentlyHandled(fingerprint: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        lastSuccessfulFingerprints.entries.removeAll { now - it.value > SUCCESS_FINGERPRINT_TTL_MILLIS }
        return lastSuccessfulFingerprints.containsKey(fingerprint)
    }

    /** 在内存中记录已成功解析页面的短期指纹。 */
    private fun rememberSuccessful(fingerprint: String) {
        lastSuccessfulFingerprints[fingerprint] = SystemClock.elapsedRealtime()
    }

    /** 依次尝试无障碍截图 OCR、Root 截图 OCR，最后才允许私有云 AI。 */
    private suspend fun recognizeWithFallbacks(input: RuleInput): Boolean {
        val settings = withContext(Dispatchers.IO) { application.repository.getSettingsSnapshot() }
        val provider = paymentProviderForPackage(input.packageName) ?: return false
        var ocrInput: RuleInput? = null
        var ocrCapture: AutoBookkeepingCapture? = null
        var retainedBitmap: Bitmap? = null
        var accessibilityScreenshotAvailable = false
        if (settings.autoLocalOcrEnabled) {
            val bitmap = takeAccessibilityScreenshot()?.let(::cropAndScaleScreenshot)
            if (bitmap != null) {
                accessibilityScreenshotAvailable = true
                val lines = localOcr.recognize(provider, bitmap)
                if (!lines.isNullOrEmpty()) {
                    ocrInput = input.copy(source = AutoCaptureSource.LOCAL_OCR, textParts = lines)
                    ocrCapture = withContext(Dispatchers.IO) { captureHandler.parseRules(ocrInput) }
                }
                if (settings.autoAiVisionEnabled) retainedBitmap = bitmap else bitmap.recycle()
            }
        }
        if (
            ocrCapture == null &&
            !accessibilityScreenshotAvailable &&
            settings.autoLocalOcrEnabled &&
            settings.autoRootOcrEnabled
        ) {
            val bitmap = takeRootScreenshotBitmap()?.let(::cropAndScaleScreenshot)
            if (bitmap != null) {
                val lines = localOcr.recognize(provider, bitmap)
                if (!lines.isNullOrEmpty()) {
                    ocrInput = input.copy(source = AutoCaptureSource.ROOT_OCR, textParts = lines)
                    ocrCapture = withContext(Dispatchers.IO) { captureHandler.parseRules(ocrInput) }
                }
                if (settings.autoAiVisionEnabled) retainedBitmap = bitmap else bitmap.recycle()
            }
        }
        if (retainedBitmap == null && settings.autoAiVisionEnabled) {
            retainedBitmap = takeAccessibilityScreenshot()?.let(::cropAndScaleScreenshot)
        }
        val finalInput = ocrInput ?: input
        return try {
            withContext(Dispatchers.IO) {
                captureHandler.handleParsed(finalInput, ocrCapture, retainedBitmap)
            }
        } finally {
            retainedBitmap?.recycle()
        }
    }

    /** 使用公开无障碍截图 API 取得一次性内存 Bitmap。 */
    private suspend fun takeAccessibilityScreenshot(): Bitmap? = suspendCancellableCoroutine { continuation ->
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                /** 转换并立即关闭系统 HardwareBuffer。 */
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val hardwareBuffer = screenshot.hardwareBuffer
                    val bitmap = try {
                        Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                    } finally {
                        hardwareBuffer.close()
                    }
                    if (continuation.isActive) continuation.resume(bitmap) else bitmap?.recycle()
                }

                /** 截图权限或系统能力不可用时交给 Root/AI 后续回退。 */
                override fun onFailure(errorCode: Int) {
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }

    /** 裁掉系统栏并把长边缩放到 OCR 需要的有限尺寸。 */
    private fun cropAndScaleScreenshot(source: Bitmap): Bitmap {
        val insets = getSystemService(WindowManager::class.java)
            .currentWindowMetrics
            .windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        val left = insets.left.coerceIn(0, source.width - 1)
        val top = insets.top.coerceIn(0, source.height - 1)
        val right = (source.width - insets.right).coerceIn(left + 1, source.width)
        val bottom = (source.height - insets.bottom).coerceIn(top + 1, source.height)
        val cropped = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        if (cropped !== source) source.recycle()
        val longSide = maxOf(cropped.width, cropped.height)
        if (longSide <= MAX_OCR_BITMAP_LONG_SIDE) return cropped
        val scale = MAX_OCR_BITMAP_LONG_SIDE.toFloat() / longSide
        val scaled = Bitmap.createScaledBitmap(
            cropped,
            (cropped.width * scale).toInt().coerceAtLeast(1),
            (cropped.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

    /** 表示防抖任务的包名与窗口边界。 */
    private data class WindowKey(val packageName: String, val windowId: Int)

    /** 表示一次不会落盘的页面节点快照。 */
    private data class PageSnapshot(val textParts: List<String>, val fingerprint: String)

    private companion object {
        const val MAX_ACCESSIBILITY_NODES = 300
        const val PAGE_DEBOUNCE_MILLIS = 500L
        const val STABILITY_READ_INTERVAL_MILLIS = 250L
        const val MAX_STABILITY_RETRIES = 3
        const val SUCCESS_FINGERPRINT_TTL_MILLIS = 10_000L
        const val MAX_OCR_BITMAP_LONG_SIDE = 1440
    }
}

/** 生成不包含原始页面内容的稳定 SHA-256 指纹。 */
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }
