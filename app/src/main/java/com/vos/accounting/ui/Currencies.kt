package com.vos.accounting.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.CURRENCY_RATE_SCALE
import com.vos.accounting.data.CurrencyEntity
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 展示预置与自定义币种，并允许创建带手动汇率的币种。
 */
@Composable
fun CurrencyScreen(
    currencies: List<CurrencyEntity>,
    selectedKey: String,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: (String, String, Long, (String) -> Unit) -> Unit,
    onUpdate: (String, String, String, Long, () -> Unit) -> Unit,
    onDelete: (String, () -> Unit) -> Unit,
    onAutoRateChange: (String, Boolean, () -> Unit) -> Unit,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingCurrencyKey by rememberSaveable { mutableStateOf<String?>(null) }
    var managedCurrencyKey by rememberSaveable { mutableStateOf<String?>(null) }
    var currencyToDeleteKey by rememberSaveable { mutableStateOf<String?>(null) }
    val editingCurrency = currencies.firstOrNull { it.key == editingCurrencyKey }
    val managedCurrency = currencies.firstOrNull { it.key == managedCurrencyKey }
    val currencyToDelete = currencies.firstOrNull { it.key == currencyToDeleteKey }
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AccountingBlurTopBar(backdrop = backdrop) {
                TopAppBar(
                    title = "选择账户币种",
                    color = Color.Transparent,
                    navigationIcon = {
                        IconButton(onClick = onBack, minWidth = 35.dp, minHeight = 35.dp) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    },
                    actions = {
                        TopBarIconAction(MiuixIcons.Add, "新增币种") {
        editingCurrencyKey = null
        showEditor = true
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                )
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 800.dp)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item { AccountSectionTitle("预置币种") }
                item {
                    CurrencyCard(
                        currencies.filter(CurrencyEntity::isBuiltin),
                        selectedKey,
                        onSelect,
                        onLongClick = { managedCurrencyKey = it.key },
                    )
                }
                item { AccountSectionTitle("自定义币种") }
                if (currencies.any { !it.isBuiltin }) {
                    item {
                        CurrencyCard(
                            currencies.filterNot(CurrencyEntity::isBuiltin),
                            selectedKey,
                            onSelect,
                            onLongClick = { managedCurrencyKey = it.key },
                        )
                    }
                } else {
                    item { EmptyCard(text = "暂无自定义币种") }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
                }
            }
        }
    }
    CurrencyEditorDialog(
        show = showEditor,
        currency = editingCurrency,
        writeInProgress = writeInProgress,
        onDismiss = {
            showEditor = false
            editingCurrencyKey = null
        },
        onConfirm = { name, symbol, rate ->
            if (editingCurrency == null) {
                onAdd(name, symbol, rate) { key ->
                    showEditor = false
                    onSelect(key)
                }
            } else {
                onUpdate(editingCurrency.key, name, symbol, rate) {
                    showEditor = false
                    editingCurrencyKey = null
                }
            }
        },
    )
    CurrencyManageDialog(
        currency = managedCurrency,
        writeInProgress = writeInProgress,
        onDismiss = { managedCurrencyKey = null },
        onEdit = {
            managedCurrencyKey = null
            editingCurrencyKey = it.key
            showEditor = true
        },
        onDelete = {
            managedCurrencyKey = null
            currencyToDeleteKey = it.key
        },
        onAutoRateChange = { currency, enabled ->
            onAutoRateChange(currency.key, enabled) {
                managedCurrencyKey = null
            }
        },
    )
    CurrencyDeleteDialog(
        currency = currencyToDelete,
        writeInProgress = writeInProgress,
        onDismiss = { currencyToDeleteKey = null },
        onConfirm = { currency ->
            onDelete(currency.key) {
                currencyToDeleteKey = null
                if (currency.key == selectedKey) onSelect("cny")
            }
        },
    )
}

/**
 * 在连续 Card 中展示一组币种。
 */
@Composable
private fun CurrencyCard(
    currencies: List<CurrencyEntity>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onLongClick: (CurrencyEntity) -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        currencies.forEach { currency ->
            BasicComponent(
                title = "${currency.symbol}  ${currency.name}",
                summary = currencyRateText(currency),
                modifier = Modifier.combinedClickable(
                    onClick = { onSelect(currency.key) },
                    onLongClick = { onLongClick(currency) },
                ),
                endActions = if (currency.key == selectedKey) {
                    {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = "已选择",
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

/**
 * 格式化币种最后成功更新时间。
 */
private fun currencyUpdatedTime(timestamp: Long): String = java.time.Instant
    .ofEpochMilli(timestamp)
    .atZone(java.time.ZoneId.systemDefault())
    .format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm", java.util.Locale.CHINA))

/**
 * 格式化一单位币种对应的人民币汇率。
 */
private fun currencyRateText(currency: CurrencyEntity): String {
    val rate = BigDecimal.valueOf(currency.rateToCnyScaled, 8).stripTrailingZeros().toPlainString()
    val code = currency.code.ifBlank { currency.name }
    val rateMode = if (currency.isBuiltin) {
        if (currency.autoRateEnabled && currency.updatedAt > 0) {
            "自动汇率 · 更新于 ${currencyUpdatedTime(currency.updatedAt)} · "
        } else if (currency.autoRateEnabled) {
            "自动汇率 · 待更新 · "
        } else {
            "手动汇率 · "
        }
    } else {
        ""
    }
    return "${rateMode}1 $code ≈ ¥$rate"
}

/**
 * 输入自定义币种名称、符号和兑人民币汇率。
 */
/** 长按币种后提供与币种来源相匹配的管理操作。 */
@Composable
private fun CurrencyManageDialog(
    currency: CurrencyEntity?,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onEdit: (CurrencyEntity) -> Unit,
    onDelete: (CurrencyEntity) -> Unit,
    onAutoRateChange: (CurrencyEntity, Boolean) -> Unit,
) {
    WindowDialog(
        show = currency != null,
        title = currency?.name ?: "管理币种",
        summary = currency?.takeIf(CurrencyEntity::isBuiltin)?.let {
            if (it.code == "CNY") "人民币是本位币，汇率固定为 1" else if (it.autoRateEnabled) "当前自动更新汇率" else "当前使用手动汇率"
        },
        onDismissRequest = onDismiss,
    ) {
        if (currency?.isBuiltin == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onAutoRateChange(currency, !currency.autoRateEnabled) },
                    modifier = Modifier.weight(1f),
                    enabled = currency.code != "CNY" && !writeInProgress,
                ) {
                    Text(if (currency.code == "CNY") "本位币" else if (currency.autoRateEnabled) "关闭自动汇率" else "开启自动汇率")
                }
                Button(
                    onClick = { onEdit(currency) },
                    modifier = Modifier.weight(1f),
                    enabled = currency.code != "CNY" && !currency.autoRateEnabled && !writeInProgress,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("编辑汇率")
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { currency?.let(onEdit) },
                    modifier = Modifier.weight(1f),
                    enabled = currency != null && !writeInProgress,
                ) {
                    Text("编辑")
                }
                Button(
                    onClick = { currency?.let(onDelete) },
                    modifier = Modifier.weight(1f),
                    enabled = currency != null && !writeInProgress,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("删除")
                }
            }
        }
    }
}

/** 二次确认删除未被账户使用的自定义币种。 */
@Composable
private fun CurrencyDeleteDialog(
    currency: CurrencyEntity?,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CurrencyEntity) -> Unit,
) {
    WindowDialog(
        show = currency != null,
        title = "删除币种",
        summary = currency?.let { "确定删除“${it.name}”吗？" },
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text("取消")
            }
            Button(
                onClick = { currency?.let(onConfirm) },
                modifier = Modifier.weight(1f),
                enabled = currency != null && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(if (writeInProgress) "删除中…" else "删除")
            }
        }
    }
}

/** 输入或编辑币种名称、符号和兑人民币汇率。 */
@Composable
private fun CurrencyEditorDialog(
    show: Boolean,
    currency: CurrencyEntity?,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var symbol by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var rate by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    LaunchedEffect(show, currency?.key) {
        if (show) {
            name = TextFieldValue(currency?.name.orEmpty())
            symbol = TextFieldValue(currency?.symbol.orEmpty())
            rate = TextFieldValue(
                currency?.let { BigDecimal.valueOf(it.rateToCnyScaled, 8).stripTrailingZeros().toPlainString() }.orEmpty(),
            )
        }
    }
    val rateScaled = parseCurrencyRate(rate.text)
    WindowDialog(show = show, title = if (currency == null) "新增币种" else "编辑币种", onDismissRequest = onDismiss) {
        TextField(name, { name = it }, Modifier.fillMaxWidth(), label = "币种名称", useLabelAsPlaceholder = true, singleLine = true, enabled = currency?.isBuiltin != true)
        TextField(symbol, { symbol = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = "币种符号", useLabelAsPlaceholder = true, singleLine = true, enabled = currency?.isBuiltin != true)
        TextField(rate, { rate = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = "1 单位币种兑换人民币", useLabelAsPlaceholder = true, singleLine = true)
        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !writeInProgress) {
                Text("取消")
            }
            Button(
                onClick = { rateScaled?.let { onConfirm(name.text, symbol.text, it) } },
                modifier = Modifier.weight(1f),
                enabled = name.text.isNotBlank() && symbol.text.isNotBlank() && rateScaled != null && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(if (writeInProgress) "保存中…" else "确定")
            }
        }
    }
}

/**
 * 将用户输入汇率转换为八位小数定点整数。
 */
private fun parseCurrencyRate(text: String): Long? = runCatching {
    text.trim().toBigDecimal()
        .multiply(BigDecimal.valueOf(CURRENCY_RATE_SCALE))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
        .takeIf { it > 0 }
}.getOrNull()
