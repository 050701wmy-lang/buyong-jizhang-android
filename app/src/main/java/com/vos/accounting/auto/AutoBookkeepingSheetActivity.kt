package com.vos.accounting.auto

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vos.accounting.AccountingApplication
import com.vos.accounting.MainActivity
import com.vos.accounting.data.AutoBookkeepingEventEntity
import com.vos.accounting.model.AutoBookkeepingStatus
import com.vos.accounting.ui.AccountingTheme
import com.vos.accounting.ui.AccountingUiState
import com.vos.accounting.ui.AccountingViewModel
import com.vos.accounting.ui.AccountingWriteErrorDialog
import com.vos.accounting.ui.ManualEntryScreen
import com.vos.accounting.ui.TopBarIconAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 以透明专用窗口承载超级岛账单编辑底部弹层。
 */
class AutoBookkeepingSheetActivity : ComponentActivity() {
    private val sheetEvent = MutableStateFlow<AutoBookkeepingEventEntity?>(null)
    private var loadEventJob: Job? = null

    /**
     * 创建透明 Compose 宿主并加载通知指定的待确认账单。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadEvent(intent)
        setContent {
            val accountingApplication = application as AccountingApplication
            val accountingViewModel: AccountingViewModel = viewModel(
                factory = AccountingViewModel.factory(
                    accountingApplication.repository,
                    accountingApplication.backupManager,
                ),
            )
            val uiState by accountingViewModel.uiState.collectAsStateWithLifecycle()
            val event by sheetEvent.collectAsStateWithLifecycle()
            val notificationManager = remember(accountingApplication) {
                AutoBookkeepingNotificationManager(
                    accountingApplication,
                    accountingApplication.repository,
                )
            }
            AccountingTheme(
                themeMode = uiState.themeMode,
                followSystemColor = uiState.followSystemColor,
            ) {
                event?.let { pendingEvent ->
                    AutoBookkeepingEditorSheet(
                        event = pendingEvent,
                        uiState = uiState,
                        viewModel = accountingViewModel,
                        notificationManager = notificationManager,
                        onFinish = ::finish,
                        onOpenFullEditor = { openFullEditor(pendingEvent.id) },
                    )
                }
                AccountingWriteErrorDialog(
                    message = uiState.writeError,
                    onDismiss = accountingViewModel::clearWriteError,
                )
            }
        }
    }

    /**
     * 在复用的专用 Activity 收到新通知点击时切换到对应账单。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadEvent(intent)
    }

    /**
     * 读取 Intent 中的事件并只允许待确认账单进入编辑弹层。
     */
    private fun loadEvent(intent: Intent?) {
        val eventId = intent
            ?.getLongExtra(EXTRA_AUTO_BOOKKEEPING_EVENT_ID, 0L)
            ?.takeIf { it > 0L }
        sheetEvent.value = null
        loadEventJob?.cancel()
        loadEventJob = lifecycleScope.launch {
            val event = eventId
                ?.let { (application as AccountingApplication).repository.findAutoBookkeepingEvent(it) }
                ?.takeIf { it.status == AutoBookkeepingStatus.PENDING }
            if (event == null) {
                finish()
            } else {
                sheetEvent.value = event
            }
        }
    }

    /**
     * 在需要管理账本或账户时退回完整应用编辑入口。
     */
    private fun openFullEditor(eventId: Long) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_AUTO_BOOKKEEPING_EVENT_ID, eventId)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
        )
        finish()
    }
}

/**
 * 在 MIUIX WindowBottomSheet 中复用手动记账编辑器并提交原待确认事件。
 */
@Composable
private fun AutoBookkeepingEditorSheet(
    event: AutoBookkeepingEventEntity,
    uiState: AccountingUiState,
    viewModel: AccountingViewModel,
    notificationManager: AutoBookkeepingNotificationManager,
    onFinish: () -> Unit,
    onOpenFullEditor: () -> Unit,
) {
    var showSheet by remember(event.id) { mutableStateOf(true) }
    var submitting by remember(event.id) { mutableStateOf(false) }
    var pendingDismissAction by remember(event.id) { mutableStateOf<(() -> Unit)?>(null) }
    val backdrop = rememberLayerBackdrop { drawContent() }
    val finishSheet = {
        pendingDismissAction = onFinish
        showSheet = false
    }
    val requestDismiss = {
        if (!uiState.writeInProgress && !submitting) {
            finishSheet()
        }
    }
    val openFullEditor = {
        if (!uiState.writeInProgress && !submitting) {
            pendingDismissAction = onOpenFullEditor
            showSheet = false
        }
    }
    LaunchedEffect(uiState.writeError) {
        if (uiState.writeError != null) {
            submitting = false
        }
    }

    WindowBottomSheet(
        show = showSheet,
        title = "记一笔",
        startAction = {
            Row(modifier = Modifier.padding(start = 20.dp)) {
                TopBarIconAction(
                    icon = MiuixIcons.Close,
                    contentDescription = "关闭",
                    onClick = requestDismiss,
                )
            }
        },
        cornerRadius = 30.dp,
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = requestDismiss,
        onDismissFinished = {
            val action = pendingDismissAction ?: onFinish
            pendingDismissAction = null
            action()
        },
        allowDismiss = !uiState.writeInProgress && !submitting,
    ) {
        ManualEntryScreen(
            uiState = uiState,
            backdrop = backdrop,
            onBack = finishSheet,
            initialDraft = event.toEditableTransactionDraft(),
            draftKey = event.id,
            allowLedgerChange = false,
            embeddedInSheet = true,
            onSave = { draft, onSaved ->
                submitting = true
                viewModel.confirmAutoBookkeepingEvent(event.id, draft) {
                    notificationManager.cancel(event.id)
                    onSaved()
                }
            },
            onAddLedger = openFullEditor,
            onManageLedgers = openFullEditor,
            onAddAccount = { openFullEditor() },
            onAddCategory = viewModel::addCategory,
        )
    }
}
