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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vos.accounting.auto.AutoBookkeepingAccessibilityService
import com.vos.accounting.auto.AutoBookkeepingNotificationListenerService
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 展示 AI 记账来源、通知隐私与系统权限状态。 */
@Composable
fun AutoBookkeepingSettingsScreen(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onProviderEnabledChange: (PaymentProvider, Boolean) -> Unit,
    onPrivacyModeChange: (NotificationPrivacyMode) -> Unit,
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
                        onCheckedChange = onEnabledChange,
                        title = "AI 记账",
                        summary = "支付后生成待确认草稿，不会直接写入正式账目",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoBookkeepingWechatEnabled,
                        onCheckedChange = { onProviderEnabledChange(PaymentProvider.WECHAT, it) },
                        title = "微信",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoBookkeepingAlipayEnabled,
                        onCheckedChange = { onProviderEnabledChange(PaymentProvider.ALIPAY, it) },
                        title = "支付宝",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.autoBookkeepingUnionPayEnabled,
                        onCheckedChange = { onProviderEnabledChange(PaymentProvider.UNIONPAY, it) },
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
            item { SectionTitle("通知隐私") }
            item {
                PrivacyModePreference(
                    selected = uiState.notificationPrivacyMode,
                    onSelect = onPrivacyModeChange,
                )
            }
        }
    }
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
