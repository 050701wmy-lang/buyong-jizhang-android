package com.vos.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vos.accounting.ui.AccountingApp

/**
 * 承载记账应用的单 Activity Compose 界面。
 */
class MainActivity : ComponentActivity() {
    /**
     * 配置边到边窗口并启动应用界面。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccountingApp()
        }
    }
}
