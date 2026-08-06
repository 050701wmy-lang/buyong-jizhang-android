package com.vos.accounting.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vos.accounting.backup.PreparedBackup
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 备份页当前等待密码确认的动作。 */
private enum class BackupPendingAction {
    CREATE,
    RESTORE,
}

/**
 * 展示数据备份与恢复入口，支持密码导出与覆盖式恢复。
 */
@Composable
fun BackupRestoreScreen(
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onExport: (String, (ByteArray) -> Unit) -> Unit,
    onParse: (ByteArray, String, (PreparedBackup) -> Unit) -> Unit,
    onApply: (PreparedBackup, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var pendingAction by rememberSaveable { mutableStateOf<BackupPendingAction?>(null) }
    var pendingBlob by remember { mutableStateOf<ByteArray?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var prepared by remember { mutableStateOf<PreparedBackup?>(null) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let {
            val bytes = pendingExportBytes
            if (bytes != null) {
                context.contentResolver.openOutputStream(it)?.use { out -> out.write(bytes) }
                status = "备份已保存"
            }
        }
        pendingExportBytes = null
    }
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { input ->
                pendingBlob = input.readBytes()
                pendingAction = BackupPendingAction.RESTORE
                showPasswordDialog = true
            }
        }
    }

    SecondaryScaffold(
        title = "数据备份与恢复",
        backdrop = backdrop,
        onBack = onBack,
    ) { innerPadding ->
        SecondaryList(innerPadding = innerPadding) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    BasicComponent(
                        title = "创建备份",
                        summary = "导出全部账本、账户、账目、币种、分类、设置与自定义封面图标",
                        modifier = Modifier.fillMaxWidth(),
                        endActions = {
                            Icon(
                                imageVector = MiuixIcons.Basic.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = {
                            if (!writeInProgress) {
                                pendingAction = BackupPendingAction.CREATE
                                showPasswordDialog = true
                            }
                        },
                    )
                    BasicComponent(
                        title = "从备份恢复",
                        summary = "选择加密备份文件并覆盖当前全部数据",
                        modifier = Modifier.fillMaxWidth(),
                        endActions = {
                            Icon(
                                imageVector = MiuixIcons.Basic.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = {
                            if (!writeInProgress) {
                                openLauncher.launch(arrayOf("*/*"))
                            }
                        },
                    )
                }
            }
            item {
                Text(
                    text = status ?: "备份文件经过密码加密；恢复会覆盖当前全部数据且不可撤销。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }

    BackupPasswordDialog(
        show = showPasswordDialog,
        writeInProgress = writeInProgress,
        onDismiss = { showPasswordDialog = false },
        onConfirm = { password ->
            showPasswordDialog = false
            when (pendingAction) {
                BackupPendingAction.CREATE -> {
                    onExport(password) { bytes ->
                        pendingExportBytes = bytes
                        createLauncher.launch("随记备份_${backupTimeLabel()}.bak")
                    }
                }
                BackupPendingAction.RESTORE -> {
                    val blob = pendingBlob
                    if (blob != null) {
                        onParse(blob, password) { parsed -> prepared = parsed }
                    }
                }
                null -> Unit
            }
            pendingAction = null
            pendingBlob = null
        },
    )
    BackupPreviewDialog(
        prepared = prepared,
        writeInProgress = writeInProgress,
        onDismiss = { prepared = null },
        onConfirm = {
            val target = prepared
            prepared = null
            if (target != null) {
                onApply(target) { status = "恢复完成" }
            }
        },
    )
}

/**
 * 输入备份密码的确认弹窗。
 */
@Composable
private fun BackupPasswordDialog(
    show: Boolean,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    LaunchedEffect(show) {
        if (show) password = TextFieldValue()
    }
    WindowDialog(
        show = show,
        title = "备份密码",
        summary = "创建备份时设置密码，恢复时需要同一密码",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = "密码",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = { onConfirm(password.text) },
                modifier = Modifier.weight(1f),
                enabled = password.text.isNotBlank() && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "处理中…" else "确认")
            }
        }
    }
}

/**
 * 恢复前展示备份摘要并二次确认覆盖。
 */
@Composable
private fun BackupPreviewDialog(
    prepared: PreparedBackup?,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val summary = prepared?.summary
    WindowDialog(
        show = summary != null,
        title = "恢复备份",
        summary = "恢复将覆盖当前全部数据且不可撤销，请确认备份内容",
        onDismissRequest = onDismiss,
    ) {
        if (summary != null) {
            Text(
                text = "创建时间：${formatBackupTime(summary.createdAt)}\n" +
                    "账本 ${summary.ledgerCount} 个 · 账户 ${summary.accountCount} 个 · " +
                    "账目 ${summary.transactionCount} 笔\n" +
                    "币种 ${summary.currencyCount} 个 · 媒体 ${summary.mediaCount} 项",
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body2,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "恢复中…" else "确认恢复")
            }
        }
    }
}

/** 生成备份文件建议名称中的时间标签。 */
private fun backupTimeLabel(): String = Instant
    .ofEpochMilli(System.currentTimeMillis())
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.CHINA))

/** 格式化备份创建时间。 */
private fun formatBackupTime(timestamp: Long): String = Instant
    .ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA))
