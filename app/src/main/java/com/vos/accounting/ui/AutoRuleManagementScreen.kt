package com.vos.accounting.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vos.accounting.auto.AutoBookkeepingRuleV1
import com.vos.accounting.auto.MAX_RULE_PACK_BYTES
import com.vos.accounting.auto.RulePackV1
import com.vos.accounting.data.AutoRulePackEntity
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/** 展示内置与用户导入的声明式规则包，并提供导入、启停和删除操作。 */
@Composable
fun AutoRuleManagementScreen(
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    viewModel: AccountingViewModel,
) {
    val context = LocalContext.current
    val storedEntities by viewModel.autoRulePacks.collectAsStateWithLifecycle()
    val displayedEntities = remember(storedEntities) {
        storedEntities.distinctBy(AutoRulePackEntity::packId)
    }
    val builtinPack = remember(context) { viewModel.readBuiltinAutoRulePack(context) }
    var resultText by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<AutoRulePackEntity?>(null) }
    var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val bytes = uri?.let { selected -> readRulePackBytes(context, selected, MAX_RULE_PACK_BYTES + 1) }
        if (bytes == null) {
            resultText = "未读取规则文件"
        } else {
            viewModel.importAutoRulePack(context, bytes) {
                resultText = "规则已导入并激活"
            }
        }
    }

    SecondaryScaffold(title = "规则管理", backdrop = backdrop, onBack = onBack) { innerPadding ->
        SecondaryList(innerPadding) {
            item { SectionTitle("规则操作") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    BasicComponent(
                        title = "导入 RulePackV1",
                        summary = resultText ?: "导入经过 schema 与线性正则校验的本地 JSON",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) },
                    )
                    BasicComponent(
                        title = "恢复内置规则",
                        summary = "删除全部用户规则包，保留随应用提供的规则",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showRestoreDialog = true },
                    )
                }
            }
            item { SectionTitle("内置规则") }
            item {
                RulePackCard(
                    title = "内置支付规则",
                    pack = builtinPack,
                    active = true,
                    builtin = true,
                )
            }
            item { SectionTitle("用户规则") }
            if (displayedEntities.isEmpty()) {
                item { EmptyCard("暂无用户规则包") }
            } else {
                items(displayedEntities, key = AutoRulePackEntity::id) { entity ->
                    val pack = remember(entity.id, entity.jsonContent) {
                        viewModel.readStoredAutoRulePack(context, entity)
                    }
                    RulePackCard(
                        title = entity.packId,
                        pack = pack,
                        active = entity.isActive,
                        builtin = false,
                        modifier = Modifier.animateItem(),
                        onActiveChange = { active ->
                            viewModel.updateAutoRulePackActive(context, entity, active) {
                                resultText = if (active) "${entity.packId} 已启用" else "${entity.packId} 已停用"
                            }
                        },
                        onDelete = { deleteTarget = entity },
                    )
                }
            }
        }
    }
    RulePackDeleteDialog(
        entity = deleteTarget,
        onDismiss = { deleteTarget = null },
        onConfirm = { entity ->
            viewModel.deleteAutoRulePack(entity.packId) {
                resultText = "${entity.packId} 已删除"
                deleteTarget = null
            }
        },
    )
    RestoreRulePacksDialog(
        show = showRestoreDialog,
        onDismiss = { showRestoreDialog = false },
        onConfirm = {
            viewModel.restoreBuiltinAutoRules {
                resultText = "已恢复内置规则"
                showRestoreDialog = false
            }
        },
    )
}

/** 展示一个规则包的状态、版本与包内规则摘要。 */
@Composable
private fun RulePackCard(
    title: String,
    pack: RulePackV1?,
    active: Boolean,
    builtin: Boolean,
    modifier: Modifier = Modifier,
    onActiveChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Card(
        modifier = modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        val summary = pack?.let { value ->
            "${value.packId} · v${value.packVersion} · ${value.rules.size} 条规则"
        } ?: "规则内容不可读取"
        if (builtin) {
            BasicComponent(
                title = title,
                summary = summary,
                modifier = Modifier.fillMaxWidth(),
                endActions = {
                    Text(
                        text = "内置",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                },
            )
        } else {
            SwitchPreference(
                checked = active,
                onCheckedChange = onActiveChange,
                title = title,
                summary = summary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        pack?.rules
            ?.sortedWith(compareBy(AutoBookkeepingRuleV1::provider, AutoBookkeepingRuleV1::type, AutoBookkeepingRuleV1::id))
            ?.forEach { rule -> RuleSummaryRow(rule) }
        if (!builtin) {
            BasicComponent(
                title = "删除规则包",
                summary = "删除后立即停止匹配其中的全部规则",
                modifier = Modifier.fillMaxWidth(),
                onClick = onDelete,
            )
        }
    }
}

/** 展示单条规则的平台、方向、来源和稳定标识。 */
@Composable
private fun RuleSummaryRow(rule: AutoBookkeepingRuleV1) {
    BasicComponent(
        title = "${ruleProviderLabel(rule.provider)}${ruleTypeLabel(rule.type)}",
        summary = "${rule.id} · ${rule.sources.sortedBy(AutoCaptureSource::ordinal).joinToString(" / ", transform = ::ruleSourceLabel)}",
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 请求删除用户规则包前进行二次确认。 */
@Composable
private fun RulePackDeleteDialog(
    entity: AutoRulePackEntity?,
    onDismiss: () -> Unit,
    onConfirm: (AutoRulePackEntity) -> Unit,
) {
    WindowDialog(
        show = entity != null,
        title = "删除规则包",
        onDismissRequest = onDismiss,
    ) {
        Text("删除 ${entity?.packId.orEmpty()} 后，其中规则将不再参与识别。")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = { entity?.let(onConfirm) },
                modifier = Modifier.weight(1f),
                enabled = entity != null,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("删除") }
        }
    }
}

/** 请求清空全部用户规则包前进行二次确认。 */
@Composable
private fun RestoreRulePacksDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(show = show, title = "恢复内置规则", onDismissRequest = onDismiss) {
        Text("全部用户导入规则包都会被删除，此操作无法撤销。")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("恢复") }
        }
    }
}

/** 从文档 URI 读取受大小限制的规则包字节。 */
private fun readRulePackBytes(context: Context, uri: android.net.Uri, limit: Int): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input -> input.readNBytes(limit) }
}.getOrNull()

/** 返回规则平台的中文名称。 */
private fun ruleProviderLabel(provider: PaymentProvider): String = when (provider) {
    PaymentProvider.WECHAT -> "微信"
    PaymentProvider.ALIPAY -> "支付宝"
    PaymentProvider.UNIONPAY -> "云闪付"
}

/** 返回规则收支方向的中文名称。 */
private fun ruleTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
}

/** 返回规则采集来源的中文名称。 */
private fun ruleSourceLabel(source: AutoCaptureSource): String = when (source) {
    AutoCaptureSource.ACCESSIBILITY -> "无障碍"
    AutoCaptureSource.NOTIFICATION -> "通知"
    AutoCaptureSource.LOCAL_OCR -> "本地 OCR"
    AutoCaptureSource.ROOT_OCR -> "Root OCR"
    AutoCaptureSource.XPOSED -> "LSPosed"
    AutoCaptureSource.CLOUD_AI -> "云 AI"
}
