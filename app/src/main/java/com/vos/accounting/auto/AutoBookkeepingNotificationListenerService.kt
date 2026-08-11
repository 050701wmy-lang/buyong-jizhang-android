package com.vos.accounting.auto

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vos.accounting.model.AutoCaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** 仅从三个支付客户端已经发布的账单通知中提取结构化字段。 */
class AutoBookkeepingNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var captureHandler: AutoBookkeepingCaptureHandler

    /** 初始化共用采集处理器。 */
    override fun onCreate() {
        super.onCreate()
        captureHandler = AutoBookkeepingCaptureHandler(this)
    }

    /** 过滤通知包名后读取标题、正文和展开正文。 */
    override fun onNotificationPosted(statusBarNotification: StatusBarNotification?) {
        val notification = statusBarNotification ?: return
        if (!isSupportedPaymentPackage(notification.packageName)) return
        val capture = parseAutoBookkeepingText(
            packageName = notification.packageName,
            textParts = notificationTextParts(notification.notification),
            source = AutoCaptureSource.NOTIFICATION,
            occurredAt = notification.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
        ) ?: return
        serviceScope.launch { captureHandler.handle(capture) }
    }

    /** 结束服务协程，避免持有通知监听实例。 */
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** 从通知 extras 中提取有限文本字段，不持久化完整 extras。 */
    private fun notificationTextParts(notification: Notification): List<String> {
        val extras = notification.extras
        return buildList {
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.let(::add)
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let(::add)
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let(::add)
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map(CharSequence::toString)
                ?.let(::addAll)
        }.filter(String::isNotBlank).distinct()
    }
}
