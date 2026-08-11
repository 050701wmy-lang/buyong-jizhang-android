package com.vos.accounting.auto

import android.content.Context
import com.vos.accounting.AccountingApplication
import com.vos.accounting.model.AutoBookkeepingCapture

/** 汇合无障碍与通知来源，共用同一去重、落草稿和通知流程。 */
class AutoBookkeepingCaptureHandler(context: Context) {
    private val application = context.applicationContext as AccountingApplication
    private val notificationManager = AutoBookkeepingNotificationManager(context, application.repository)

    /** 保存结构化采集结果，并在生成或合并待确认事件后刷新通知。 */
    suspend fun handle(capture: AutoBookkeepingCapture) {
        val event = application.repository.captureAutoBookkeeping(capture) ?: return
        notificationManager.post(event.id)
    }
}
