package com.vos.accounting.auto

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.vos.accounting.model.AutoCaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/** 仅从三个支付客户端的无障碍事件与窗口节点提取账单结构化字段。 */
class AutoBookkeepingAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var captureHandler: AutoBookkeepingCaptureHandler

    /** 初始化共用采集处理器。 */
    override fun onCreate() {
        super.onCreate()
        captureHandler = AutoBookkeepingCaptureHandler(this)
    }

    /** 在受支持窗口变化时平铺当前事件与节点文本并尝试提取账单。 */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (!isSupportedPaymentPackage(packageName)) return
        val eventText = buildList {
            event.text.mapNotNullTo(this) { value ->
                value?.toString()?.trim()?.takeIf(String::isNotEmpty)
            }
            event.contentDescription?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        }
        val nodeText = rootInActiveWindow?.let(::collectVisibleText).orEmpty()
        val textParts = (eventText + nodeText).distinct()
        val capture = parseAutoBookkeepingText(
            packageName = packageName,
            textParts = textParts,
            source = AutoCaptureSource.ACCESSIBILITY,
            occurredAt = System.currentTimeMillis(),
        ) ?: return
        serviceScope.launch { captureHandler.handle(capture) }
    }

    /** 系统要求中断服务时不保留任何页面内容。 */
    override fun onInterrupt() = Unit

    /** 结束服务协程，避免持有系统服务实例。 */
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** 以固定节点上限读取可见文字和内容描述，不保存节点树。 */
    private fun collectVisibleText(root: AccessibilityNodeInfo): List<String> {
        val values = ArrayList<String>()
        val nodes = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (nodes.isNotEmpty() && visited < MAX_ACCESSIBILITY_NODES) {
            val node = nodes.removeFirst()
            visited += 1
            if (node.isVisibleToUser) {
                node.text?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(values::add)
                node.contentDescription?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(values::add)
            }
            repeat(node.childCount) { index ->
                node.getChild(index)?.let(nodes::addLast)
            }
        }
        return values.distinct()
    }

    private companion object {
        const val MAX_ACCESSIBILITY_NODES = 300
    }
}
