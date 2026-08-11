package com.vos.accounting

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vos.accounting.ui.AccountingApp
import com.vos.accounting.auto.EXTRA_AUTO_BOOKKEEPING_EVENT_ID
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 承载记账应用的单 Activity Compose 界面。
 */
class MainActivity : ComponentActivity() {
    private val autoBookkeepingEventId = MutableStateFlow<Long?>(null)

    /**
     * 配置边到边窗口并启动应用界面。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeAutoBookkeepingIntent(intent)
        enableEdgeToEdge()
        setContent {
            val eventId by autoBookkeepingEventId.collectAsStateWithLifecycle()
            AccountingApp(
                initialAutoBookkeepingEventId = eventId,
                onInitialAutoBookkeepingEventConsumed = { autoBookkeepingEventId.value = null },
            )
        }
    }

    /** 接收已有 Activity 上的新通知点击并路由到对应草稿。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAutoBookkeepingIntent(intent)
    }

    /** 从通知 Intent 中读取一次性自动账单标识。 */
    private fun consumeAutoBookkeepingIntent(intent: Intent?) {
        autoBookkeepingEventId.value = intent
            ?.getLongExtra(EXTRA_AUTO_BOOKKEEPING_EVENT_ID, 0L)
            ?.takeIf { it > 0 }
    }
}
