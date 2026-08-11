package com.vos.accounting.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vos.accounting.AccountingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 通知中确认入账操作的广播 action。 */
const val ACTION_CONFIRM_AUTO_BOOKKEEPING = "com.vos.accounting.auto_confirm"

/** 通知中忽略账单操作的广播 action。 */
const val ACTION_IGNORE_AUTO_BOOKKEEPING = "com.vos.accounting.auto_ignore"

/** 在应用进程被回收后仍可处理通知确认与忽略操作。 */
class AutoBookkeepingActionReceiver : BroadcastReceiver() {
    /** 异步完成幂等确认或忽略，并在成功后移除通知。 */
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_AUTO_BOOKKEEPING_EVENT_ID, 0L)
        if (eventId <= 0) return
        val result = goAsync()
        val application = context.applicationContext as AccountingApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                when (intent.action) {
                    ACTION_CONFIRM_AUTO_BOOKKEEPING -> application.repository.confirmAutoBookkeepingEvent(eventId)
                    ACTION_IGNORE_AUTO_BOOKKEEPING -> application.repository.ignoreAutoBookkeepingEvent(eventId)
                    else -> return@runCatching
                }
                AutoBookkeepingNotificationManager(context, application.repository).cancel(eventId)
            }
            result.finish()
        }
    }
}
