package com.vos.accounting.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vos.accounting.auto.AutoBookkeepingAccessibilityService
import com.vos.accounting.auto.AutoBookkeepingNotificationListenerService
import com.vos.accounting.auto.LocalOcrModels
import com.vos.accounting.data.AutoBookkeepingEventEntity
import com.vos.accounting.model.NotificationPrivacyMode
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
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
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import com.vos.accounting.auto.HOOK_STATUS_ACTIVE
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 展示 AI 记账来源、通知隐私与系统权限状态。 */
@Composable
fun AutoBookkeepingSettingsScreen(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onOpenRuleManagement: () -> Unit,
    viewModel: AccountingViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionRefresh += 1 }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationGranted = remember(permissionRefresh) { notificationPermissionGranted(context) }
    val listenerGranted = remember(permissionRefresh) { notificationListenerGranted(context) }
    val accessibilityGranted = remember(permissionRefresh) { accessibilityServiceGranted(context) }
    var rootTestResult by rememberSaveable { mutableStateOf<String?>(null) }
    var aiTestResult by rememberSaveable { mutableStateOf<String?>(null) }
    var showAiConfiguration by rememberSaveable { mutableStateOf(false) }
    var showOcrRisk by rememberSaveable { mutableStateOf(false) }
    var showVisionRisk by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val ocrModels = remember(context) { LocalOcrModels(context) }
    var ocrModelsInstalled by remember { mutableStateOf(ocrModels.isInstalled()) }
    var ocrDownloadInProgress by remember { mutableStateOf(false) }
    var ocrDownloadFailed by rememberSaveable { mutableStateOf(false) }
    SecondaryScaffold(title = "AI 记账", backdrop = backdrop, onBack = onBack) { innerPadding ->
        SecondaryList(innerPadding) {
            item { SectionTitle("功能") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SwitchPreference(
                        checked = uiState.autoBookkeepingEnabled,
                        onCheckedChange = viewModel::updateAutoBookkeepingEnabled,
                        title = "AI 记账",
                        summary = "支付后生成待确认草稿，不会直接写入正式账目",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoBookkeepingWechatEnabled,
                        onCheckedChange = { viewModel.updateAutoBookkeepingProviderEnabled(PaymentProvider.WECHAT, it) },
                        title = "微信",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoBookkeepingAlipayEnabled,
                        onCheckedChange = { viewModel.updateAutoBookkeepingProviderEnabled(PaymentProvider.ALIPAY, it) },
                        title = "支付宝",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoBookkeepingUnionPayEnabled,
                        onCheckedChange = { viewModel.updateAutoBookkeepingProviderEnabled(PaymentProvider.UNIONPAY, it) },
                        title = "云闪付",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item { SectionTitle("权限") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    PermissionPreference(
                        title = "账单通知",
                        granted = notificationGranted,
                        onClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    )
                    PermissionPreference(
                        title = "通知读取",
                        granted = listenerGranted,
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    )
                    PermissionPreference(
                        title = "无障碍读取",
                        granted = accessibilityGranted,
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
                }
            }
            item {
                Text(
                    text = "侧载后若系统提示“受限制的设置”，请先在应用信息页右上角允许受限制设置，再返回开启无障碍。仅处理三个支付应用的简体中文结果页。",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            item { SectionTitle("识别模式") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SwitchPreference(
                        checked = uiState.autoLocalOcrEnabled && ocrModelsInstalled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showOcrRisk = true
                            } else {
                                viewModel.updateAutoRootOcrEnabled(false)
                                viewModel.updateAutoLocalOcrEnabled(false)
                            }
                        },
                        title = "本地 OCR",
                        summary = when {
                            ocrDownloadInProgress -> "正在下载 OCR 组件（约 11 MB）…"
                            ocrDownloadFailed -> "下载失败，请检查网络后重试"
                            !ocrModelsInstalled -> "首次开启时下载约 11 MB OCR 组件"
                            else -> "规则缺少关键字段时，在内存中识别支付页截图"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !ocrDownloadInProgress,
                    )
                    SwitchPreference(
                        checked = uiState.autoRootOcrEnabled &&
                            uiState.autoLocalOcrEnabled &&
                            ocrModelsInstalled,
                        onCheckedChange = viewModel::updateAutoRootOcrEnabled,
                        title = "Root 截图兜底",
                        summary = "仅执行固定 su -c screencap -p，不读取支付应用数据",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.autoLocalOcrEnabled && ocrModelsInstalled && !ocrDownloadInProgress,
                    )
                    BasicComponent(
                        title = "测试 Root 截图",
                        summary = rootTestResult ?: "执行一次不落盘的内存截图测试",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            rootTestResult = "测试中…"
                            viewModel.testRootOcrAccess { passed ->
                                rootTestResult = if (passed) "授权与截图可用" else "不可用或未授权"
                            }
                        },
                    )
                    SwitchPreference(
                        checked = uiState.autoXposedEnabled,
                        onCheckedChange = viewModel::updateAutoXposedEnabled,
                        title = "LSPosed Hook",
                        summary = hookHeartbeatSummary(uiState),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item { SectionTitle("声明式规则") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    BasicComponent(
                        title = "规则管理",
                        summary = "查看内置规则，导入、启停或删除用户规则包",
                        modifier = Modifier.fillMaxWidth(),
                        endActions = { PreferenceArrow() },
                        onClick = onOpenRuleManagement,
                    )
                }
            }
            item { SectionTitle("私有云 AI") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SwitchPreference(
                        checked = uiState.autoCloudAiEnabled,
                        onCheckedChange = viewModel::updateAutoCloudAiEnabled,
                        title = "OpenAI 兼容服务",
                        summary = "仅在确定性规则或 OCR 缺字段时请求",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BasicComponent(
                        title = "端点与模型",
                        summary = if (uiState.autoAiBaseUrl.isBlank()) {
                            "尚未配置"
                        } else {
                            "${uiState.autoAiBaseUrl} · ${uiState.autoAiModel.ifBlank { "未选择模型" }}"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        endActions = { PreferenceArrow() },
                        onClick = { showAiConfiguration = true },
                    )
                    BasicComponent(
                        title = "测试 AI 连接",
                        summary = aiTestResult ?: "验证端点、鉴权与严格 JSON 响应",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            aiTestResult = "测试中…"
                            viewModel.testAutoAiConnection { passed ->
                                aiTestResult = if (passed) "连接成功" else "连接或响应格式不可用"
                            }
                        },
                    )
                    SwitchPreference(
                        checked = uiState.autoAiVisionEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) showVisionRisk = true else viewModel.updateAutoAiVisionEnabled(false)
                        },
                        title = "视觉识别",
                        summary = "允许上传裁剪压缩后的支付页截图；默认关闭",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoAiAllowInsecureLanHttp,
                        onCheckedChange = viewModel::updateAutoAiAllowInsecureLanHttp,
                        title = "允许局域网 HTTP",
                        summary = "仅回环或私有地址；不会绕过证书错误",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoAiAllowOneTapConfirm,
                        onCheckedChange = viewModel::updateAutoAiAllowOneTapConfirm,
                        title = "允许 AI 草稿一键确认",
                        summary = "仍要求无冲突且账户、分类均来自已确认映射",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item { SectionTitle("通知隐私") }
            item {
                PrivacyModePreference(
                    selected = uiState.notificationPrivacyMode,
                    onSelect = viewModel::updateNotificationPrivacyMode,
                )
            }
        }
    }
    AutoAiConfigurationDialog(
        show = showAiConfiguration,
        baseUrl = uiState.autoAiBaseUrl,
        model = uiState.autoAiModel,
        onDismiss = { showAiConfiguration = false },
        onSave = { baseUrl, model, apiKey ->
            viewModel.saveAutoAiConfiguration(baseUrl, model, apiKey) {
                showAiConfiguration = false
            }
        },
    )
    RiskConfirmationDialog(
        show = showOcrRisk,
        title = "开启本地截图识别",
        message = "首次开启会下载约 11 MB 的 PP-OCRv5 模型。支付页截图只在内存中识别，截图和 OCR 原文不会写入文件、日志、数据库或备份。",
        onDismiss = { showOcrRisk = false },
        onConfirm = {
            showOcrRisk = false
            if (ocrModelsInstalled) {
                viewModel.updateAutoLocalOcrEnabled(true)
            } else {
                ocrDownloadInProgress = true
                ocrDownloadFailed = false
                coroutineScope.launch {
                    try {
                        ocrModels.install()
                        ocrModelsInstalled = true
                        viewModel.updateAutoLocalOcrEnabled(true)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        ocrDownloadFailed = true
                    } finally {
                        ocrDownloadInProgress = false
                    }
                }
            }
        },
    )
    RiskConfirmationDialog(
        show = showVisionRisk,
        title = "允许视觉上传",
        message = "支付页截图会被裁剪、压缩后发送到你配置的私有 AI 服务。截图可能包含商户、金额和账户信息，请确认服务端可信。",
        onDismiss = { showVisionRisk = false },
        onConfirm = {
            showVisionRisk = false
            viewModel.updateAutoAiVisionEnabled(true)
        },
    )
}

/** 显示私有 AI 的端点、模型与 Keystore 密钥编辑对话框。 */
@Composable
private fun AutoAiConfigurationDialog(
    show: Boolean,
    baseUrl: String,
    model: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    var editedBaseUrl by remember(show, baseUrl) { mutableStateOf(baseUrl) }
    var editedModel by remember(show, model) { mutableStateOf(model) }
    var editedApiKey by remember(show) { mutableStateOf("") }
    WindowDialog(
        show = show,
        title = "私有 AI 配置",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = editedBaseUrl,
            onValueChange = { editedBaseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Base URL",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        TextField(
            value = editedModel,
            onValueChange = { editedModel = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            label = "模型",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        TextField(
            value = editedApiKey,
            onValueChange = { editedApiKey = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            label = "API Key（留空不修改）",
            useLabelAsPlaceholder = true,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onSave(editedBaseUrl, editedModel, "") },
                modifier = Modifier.weight(1f),
            ) { Text("清除密钥") }
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = {
                    onSave(editedBaseUrl, editedModel, editedApiKey.takeIf(String::isNotBlank))
                },
                modifier = Modifier.weight(1f),
                enabled = editedBaseUrl.isNotBlank() && editedModel.isNotBlank(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("保存") }
        }
    }
}

/** 在首次开启截图或视觉上传前展示明确风险确认。 */
@Composable
private fun RiskConfirmationDialog(
    show: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(show = show, title = title, onDismissRequest = onDismiss) {
        Text(
            text = message,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("确认开启") }
        }
    }
}

/** 显示两个 Hook 平台是否已在目标进程启动后送达心跳。 */
private fun hookHeartbeatSummary(uiState: AccountingUiState): String = listOf(
    PaymentProvider.WECHAT to "微信",
    PaymentProvider.ALIPAY to "支付宝",
).joinToString("；") { (provider, label) ->
    val heartbeat = uiState.autoHookHeartbeats.firstOrNull { it.provider == provider }
    when {
        heartbeat == null -> "$label 未收到心跳，请重启目标应用"
        heartbeat.status != HOOK_STATUS_ACTIVE -> "$label 旧心跳，请重启目标应用"
        heartbeat.lastCaptureAt != null -> "$label 已激活，最近 ${formatAutoEventTime(heartbeat.lastCaptureAt)}"
        else -> "$label ${heartbeat.appVersion} 已激活"
    }
}

/** 显示统一尺寸与颜色的 Preference 行尾箭头。 */
@Composable
private fun PreferenceArrow() {
    Icon(
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        modifier = Modifier.size(width = 10.dp, height = 16.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

/** 展示单项系统权限状态和设置入口。 */
@Composable
private fun PermissionPreference(title: String, granted: Boolean, onClick: () -> Unit) {
    BasicComponent(
        title = title,
        summary = if (granted) "已开启" else "未开启，点击前往系统设置",
        modifier = Modifier.fillMaxWidth(),
        endActions = {
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = onClick,
    )
}

/** 展示三档通知隐私选项。 */
@Composable
private fun PrivacyModePreference(
    selected: NotificationPrivacyMode,
    onSelect: (NotificationPrivacyMode) -> Unit,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        BasicComponent(
            title = "通知内容",
            summary = privacyModeLabel(selected),
            modifier = Modifier.fillMaxWidth(),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(width = 10.dp, height = 16.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            },
            onClick = { showSheet = true },
        )
    }
    AutoSelectionSheet(
        show = showSheet,
        title = "通知内容",
        options = NotificationPrivacyMode.entries.map { it.ordinal.toLong() to privacyModeLabel(it) },
        selectedId = selected.ordinal.toLong(),
        onDismiss = { showSheet = false },
        onSelect = { value ->
            onSelect(NotificationPrivacyMode.entries[value.toInt()])
            showSheet = false
        },
    )
}

/** 展示全部待确认草稿，并提供确认、编辑和忽略入口。 */
@Composable
fun AutoBookkeepingPendingScreen(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onConfirm: (Long) -> Unit,
    onIgnore: (Long) -> Unit,
) {
    SecondaryScaffold(title = "待确认", backdrop = backdrop, onBack = onBack) { innerPadding ->
        SecondaryList(innerPadding) {
            if (uiState.pendingAutoBookkeepingEvents.isEmpty()) {
                item { EmptyCard("没有待确认账单") }
            } else {
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        uiState.pendingAutoBookkeepingEvents.forEach { event ->
                            AutoBookkeepingPendingRow(event, uiState, onEdit, onConfirm, onIgnore)
                        }
                    }
                }
            }
        }
    }
}

/** 展示单条待确认账单及其可用操作。 */
@Composable
private fun AutoBookkeepingPendingRow(
    event: AutoBookkeepingEventEntity,
    uiState: AccountingUiState,
    onEdit: (Long) -> Unit,
    onConfirm: (Long) -> Unit,
    onIgnore: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "${autoProviderLabel(event.provider)}${autoTypeLabel(event.type)}  ${formatAutoAmount(event.amountMinor)}",
            style = MiuixTheme.textStyles.body1,
        )
        Text(
            text = autoEventSummary(event, uiState),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { onEdit(event.id) }, modifier = Modifier.weight(1f)) { Text("编辑") }
            Button(onClick = { onIgnore(event.id) }, modifier = Modifier.weight(1f)) { Text("忽略") }
            if (event.canConfirm) {
                Button(
                    onClick = { onConfirm(event.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) { Text("确认入账") }
            }
        }
    }
}

/** 在 MIUIX 底部弹层中展示一组固定单选项。 */
@Composable
private fun AutoSelectionSheet(
    show: Boolean,
    title: String,
    options: List<Pair<Long, String>>,
    selectedId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismiss,
        cornerRadius = 30.dp,
        insideMargin = DpSize(0.dp, 20.dp),
        allowDismiss = true,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.padding(horizontal = 20.dp), insideMargin = PaddingValues(0.dp)) {
                options.forEach { (id, label) ->
                    BasicComponent(
                        title = label,
                        endActions = {
                            if (id == selectedId) {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = "当前选项",
                                    modifier = Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { onSelect(id) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp).navigationBarsPadding())
        }
    }
}

/** 返回通知隐私模式的展示文案。 */
private fun privacyModeLabel(mode: NotificationPrivacyMode): String = when (mode) {
    NotificationPrivacyMode.HIDE_ON_LOCK_SCREEN -> "锁屏隐藏详情"
    NotificationPrivacyMode.SHOW_DETAILS -> "始终显示详情"
    NotificationPrivacyMode.HIDE_DETAILS -> "始终仅提示"
}

/** 判断应用通知运行时权限是否已经授予。 */
private fun notificationPermissionGranted(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** 判断本应用通知监听服务是否已在系统设置中启用。 */
private fun notificationListenerGranted(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    val component = ComponentName(context, AutoBookkeepingNotificationListenerService::class.java)
    return enabled.split(':').any { it == component.flattenToString() }
}

/** 判断本应用无障碍采集服务是否已在系统设置中启用。 */
private fun accessibilityServiceGranted(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            val service = info.resolveInfo.serviceInfo
            service.packageName == context.packageName &&
                service.name == AutoBookkeepingAccessibilityService::class.java.name
        }
}

/** 返回自动账单平台的中文名称。 */
private fun autoProviderLabel(provider: PaymentProvider): String = when (provider) {
    PaymentProvider.WECHAT -> "微信"
    PaymentProvider.ALIPAY -> "支付宝"
    PaymentProvider.UNIONPAY -> "云闪付"
}

/** 返回自动账单类型的中文名称。 */
private fun autoTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
}

/** 返回待确认列表中的商户、分类、账户和时间摘要。 */
private fun autoEventSummary(event: AutoBookkeepingEventEntity, uiState: AccountingUiState): String =
    listOfNotNull(
        event.merchant.ifBlank { null },
        event.categoryId?.let { id -> uiState.categories.firstOrNull { it.id == id }?.name },
        event.accountId?.let { id -> uiState.accounts.firstOrNull { it.id == id }?.name },
        formatAutoEventTime(event.occurredAt),
    ).joinToString(" · ")

/** 格式化自动账单金额并附带人民币符号。 */
private fun formatAutoAmount(amountMinor: Long): String = "¥${formatAutoAmountValue(amountMinor)}"

/** 格式化最小货币单位为可编辑十进制文本。 */
private fun formatAutoAmountValue(amountMinor: Long): String =
    BigDecimal.valueOf(amountMinor, 2).setScale(2).toPlainString()

/** 格式化自动账单发生时间。 */
private fun formatAutoEventTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))
