package com.vos.accounting.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.LedgerEntity
import com.vos.accounting.data.LedgerRecord
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

const val CUSTOM_LEDGER_COVER_PREFIX = "cover_uri:"
const val CROPPED_LEDGER_COVER_PREFIX = "cover_file:"

/** 定义内置账本封面的标识与明暗倾向。 */
data class LedgerCoverOption(
    val key: String,
    val name: String,
)

val ledgerCoverOptions = listOf(
    LedgerCoverOption("cover_ocean", "海岸"),
    LedgerCoverOption("cover_sunset", "落日"),
    LedgerCoverOption("cover_mint", "薄荷"),
    LedgerCoverOption("cover_night", "夜色"),
    LedgerCoverOption("cover_gold", "鎏金"),
    LedgerCoverOption("cover_sky", "晴空"),
)

/** 绘制可复用于账本卡片和主页面 Hero 的账本封面。 */
@Composable
fun LedgerCover(
    coverKey: String,
    modifier: Modifier = Modifier,
) {
    if (coverKey.startsWith(CUSTOM_LEDGER_COVER_PREFIX) || coverKey.startsWith(CROPPED_LEDGER_COVER_PREFIX)) {
        val context = LocalContext.current
        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, coverKey) {
            value = runCatching {
                if (coverKey.startsWith(CROPPED_LEDGER_COVER_PREFIX)) {
                    File(coverKey.removePrefix(CROPPED_LEDGER_COVER_PREFIX)).inputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } else {
                    context.contentResolver.openInputStream(android.net.Uri.parse(coverKey.removePrefix(CUSTOM_LEDGER_COVER_PREFIX)))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }
            }.getOrNull()
        }
        bitmap?.let {
            Image(it, null, modifier, contentScale = ContentScale.Crop)
        } ?: Box(modifier.background(MiuixTheme.colorScheme.primary))
    } else {
        Box(modifier.background(ledgerCoverBrush(coverKey)))
    }
}

/** 根据封面平均亮度自动选择前景文字颜色。 */
@Composable
fun ledgerCoverTextColor(coverKey: String): Color {
    val defaultColor = when (coverKey) {
        "cover_mint", "cover_sky" -> Color(0xFF20242A)
        else -> Color.White
    }
    if (!coverKey.startsWith(CUSTOM_LEDGER_COVER_PREFIX) && !coverKey.startsWith(CROPPED_LEDGER_COVER_PREFIX)) return defaultColor
    val context = LocalContext.current
    return produceState(defaultColor, coverKey) {
        value = runCatching {
            val stream = if (coverKey.startsWith(CROPPED_LEDGER_COVER_PREFIX)) {
                File(coverKey.removePrefix(CROPPED_LEDGER_COVER_PREFIX)).inputStream()
            } else {
                context.contentResolver.openInputStream(
                    android.net.Uri.parse(coverKey.removePrefix(CUSTOM_LEDGER_COVER_PREFIX)),
                )
            }
            stream?.use { input ->
                BitmapFactory.decodeStream(input)?.let { bitmap ->
                    var luminance = 0.0
                    var count = 0
                    val stepX = (bitmap.width / 12).coerceAtLeast(1)
                    val stepY = (bitmap.height / 12).coerceAtLeast(1)
                    for (y in 0 until bitmap.height step stepY) {
                        for (x in 0 until bitmap.width step stepX) {
                            val pixel = bitmap.getPixel(x, y)
                            luminance += (0.299 * android.graphics.Color.red(pixel) + 0.587 * android.graphics.Color.green(pixel) + 0.114 * android.graphics.Color.blue(pixel)) / 255.0
                            count++
                        }
                    }
                    if (count > 0 && luminance / count > 0.58) Color(0xFF20242A) else Color.White
                }
            }
        }.getOrNull() ?: defaultColor
    }.value
}

/** 返回内置封面对应的轻量渐变。 */
private fun ledgerCoverBrush(key: String): Brush = when (key) {
    "cover_sunset" -> Brush.linearGradient(listOf(Color(0xFFE58B63), Color(0xFF6D6AAE)))
    "cover_mint" -> Brush.linearGradient(listOf(Color(0xFFC8E7D3), Color(0xFF82B8B2)))
    "cover_night" -> Brush.linearGradient(listOf(Color(0xFF182A4A), Color(0xFF425779)))
    "cover_gold" -> Brush.linearGradient(listOf(Color(0xFF332B23), Color(0xFFB38945)))
    "cover_sky" -> Brush.linearGradient(listOf(Color(0xFFB9DCF4), Color(0xFFE8E4C5)))
    else -> Brush.linearGradient(listOf(Color(0xFF3F86B8), Color(0xFF86C9C8)))
}

/** 展示账本卡片网格并允许切换或编辑账本。 */
@Composable
fun LedgerScreen(
    ledgers: List<LedgerRecord>,
    currentLedgerId: Long,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSelect: (Long) -> Unit,
) {
    SecondaryScaffold(
        title = "选择账本",
        backdrop = backdrop,
        onBack = onBack,
        actions = { LedgerAddAction(onAdd) },
    ) { innerPadding ->
        val visible = ledgers.filterNot(LedgerRecord::isHidden)
        val hidden = ledgers.filter(LedgerRecord::isHidden)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            item { LedgerGrid(visible, currentLedgerId, onEdit, onSelect) }
            if (hidden.isNotEmpty()) {
                item { LedgerSectionTitle("隐藏账本") }
                item { LedgerGrid(hidden, currentLedgerId, onEdit, onSelect) }
            }
            item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
        }
    }
}

/** 显示两列账本卡片。 */
@Composable
private fun LedgerGrid(
    ledgers: List<LedgerRecord>,
    currentLedgerId: Long,
    onEdit: (Long) -> Unit,
    onSelect: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ledgers.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { ledger ->
                    LedgerCard(ledger, ledger.id == currentLedgerId, onEdit, onSelect, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 显示一张含封面、名称和明细数量的账本卡片。 */
@Composable
private fun LedgerCard(
    ledger: LedgerRecord,
    current: Boolean,
    onEdit: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    modifier: Modifier,
) {
    Card(modifier = modifier.combinedClickable(onClick = { onSelect(ledger.id) }, onLongClick = { onEdit(ledger.id) }), insideMargin = PaddingValues(0.dp)) {
        val textColor = ledgerCoverTextColor(ledger.coverKey)
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).squircleClip(16.dp)) {
            LedgerCover(ledger.coverKey, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.14f)))
            if (current) {
                Text("● 当前账本", Modifier.padding(10.dp), color = textColor, style = MiuixTheme.textStyles.body2)
            }
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(ledger.name, Modifier.weight(1f), color = textColor, fontWeight = FontWeight.Medium, style = MiuixTheme.textStyles.body1)
                Text("${ledger.transactionCount} 笔明细", color = textColor.copy(alpha = 0.86f), style = MiuixTheme.textStyles.footnote1)
            }
        }
    }
}

/** 显示账本页分区标题。 */
@Composable
private fun LedgerSectionTitle(text: String) {
    Text(text, Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp), color = MiuixTheme.colorScheme.primary)
}

/** 显示账本页右上角新增操作。 */
@Composable
private fun RowScope.LedgerAddAction(onClick: () -> Unit) {
    IconButton(onClick, backgroundColor = Color.Transparent, minWidth = TOP_BAR_ACTION_BUTTON_SIZE, minHeight = TOP_BAR_ACTION_BUTTON_SIZE) {
        Icon(MiuixIcons.Add, "添加账本", Modifier.size(TOP_BAR_ACTION_ICON_SIZE))
    }
}

/** 添加或编辑账本名称、封面、本位币和隐藏状态。 */
@Composable
fun LedgerEditorScreen(
    ledger: LedgerEntity?,
    currencies: List<CurrencyEntity>,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onAdd: (String, String, Boolean, String, Boolean, (Long) -> Unit) -> Unit,
    onUpdate: (LedgerEntity, () -> Unit) -> Unit,
    onDelete: (Long, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable(ledger?.id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(ledger?.name.orEmpty())) }
    var coverKey by rememberSaveable(ledger?.id) { mutableStateOf(ledger?.coverKey ?: "cover_ocean") }
    var currencyKey by rememberSaveable(ledger?.id) { mutableStateOf(ledger?.baseCurrencyKey ?: "cny") }
    var hidden by rememberSaveable(ledger?.id) { mutableStateOf(ledger?.isHidden ?: false) }
    var showCovers by rememberSaveable { mutableStateOf(false) }
    var cropSourceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showCurrencies by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            showCovers = false
            cropSourceKey = "$CUSTOM_LEDGER_COVER_PREFIX$it"
        }
    }
    val save = {
        if (ledger == null) {
            onAdd(name.text, coverKey, true, currencyKey, hidden) { onBack() }
        } else {
            onUpdate(ledger.copy(name = name.text, coverKey = coverKey, useLightText = true, baseCurrencyKey = currencyKey, isHidden = hidden), onBack)
        }
    }
    SecondaryScaffold(
        title = if (ledger == null) "添加账本" else "编辑账本",
        backdrop = backdrop,
        onBack = onBack,
        bottomBar = {
            if (ledger == null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding()) {
                    Button(save, Modifier.fillMaxWidth(), enabled = name.text.isNotBlank() && !writeInProgress, colors = ButtonDefaults.buttonColorsPrimary()) { Text("保存") }
                }
            } else {
                LedgerEditorBottomBar(
                    writeInProgress = writeInProgress,
                    saveEnabled = name.text.isNotBlank() && !writeInProgress,
                    onDelete = { showDelete = true },
                    onSave = save,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = innerPadding.calculateTopPadding())) {
            item { Spacer(Modifier.height(12.dp)) }
            item { TextField(name, { name = it }, Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), label = "账本名称", useLabelAsPlaceholder = false, singleLine = true) }
            item {
            Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), insideMargin = PaddingValues(0.dp)) {
                    BasicComponent(title = "账本封面", onClick = { showCovers = true }, endActions = { LedgerCover(coverKey, Modifier.size(width = 72.dp, height = 42.dp).squircleClip(10.dp)) })
                    BasicComponent(title = "本位币", summary = currencies.firstOrNull { it.key == currencyKey }?.name, onClick = { showCurrencies = true })
                    top.yukonga.miuix.kmp.preference.SwitchPreference(checked = hidden, onCheckedChange = { hidden = it }, title = "隐藏账本")
                }
            }
            item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
        }
    }
    WindowDialog(show = showCovers, title = "选择账本封面", onDismissRequest = { showCovers = false }) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(264.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ledgerCoverOptions.size) { index ->
                val option = ledgerCoverOptions[index]
                Box(Modifier.aspectRatio(16f / 9f).squircleClip(12.dp).clickable { coverKey = option.key; showCovers = false }) {
                    LedgerCover(option.key, Modifier.fillMaxSize())
                    Text(option.name, Modifier.align(Alignment.BottomStart).padding(6.dp), color = ledgerCoverTextColor(option.key), style = MiuixTheme.textStyles.footnote1)
                }
            }
        }
        Button({ launcher.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColorsPrimary()) { Text("从相册选择") }
    }
    LedgerCoverCropDialog(
        sourceKey = cropSourceKey,
        onDismiss = { cropSourceKey = null },
        onConfirm = { croppedKey ->
            coverKey = croppedKey
            cropSourceKey = null
        },
    )
    WindowDialog(show = showCurrencies, title = "选择本位币", onDismissRequest = { showCurrencies = false }) {
        currencies.forEach { currency ->
            BasicComponent(title = currency.name, summary = currency.code.ifBlank { currency.symbol }, onClick = { currencyKey = currency.key; showCurrencies = false })
        }
    }
    WindowDialog(
        show = showDelete,
        title = "删除账本",
        summary = "确定删除“${ledger?.name.orEmpty()}”吗？账本中的明细不会被删除。",
        onDismissRequest = { showDelete = false },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button({ showDelete = false }, Modifier.weight(1f), enabled = !writeInProgress) { Text("取消") }
            Button(
                onClick = { ledger?.let { onDelete(it.id) { showDelete = false; onBack() } } },
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("删除") }
        }
    }
}

/** 在固定 21:9 视窗中裁剪相册图片并保存为账本封面。 */
@Composable
private fun LedgerCoverCropDialog(
    sourceKey: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sourceBitmap by produceState<Bitmap?>(null, sourceKey) {
        value = sourceKey?.let { key ->
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(key.removePrefix(CUSTOM_LEDGER_COVER_PREFIX)))?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    var scale by rememberSaveable(sourceKey) { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable(sourceKey) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(sourceKey) { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { _, zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offsetX += panChange.x
        offsetY += panChange.y
    }
    WindowDialog(show = sourceKey != null, title = "裁剪账本封面", onDismissRequest = onDismiss) {
        Text("拖动或双指缩放图片，裁剪框固定为 21:9", color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .aspectRatio(LEDGER_HERO_ASPECT_RATIO)
                .background(Color.Black.copy(alpha = 0.08f))
                .squircleClip(14.dp)
                .transformable(transformState),
        ) {
            val bitmap = sourceBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surfaceVariant))
            }
        }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onDismiss, Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = {
                    val bitmap = sourceBitmap ?: return@Button
                    scope.launch {
                        val key = withContext(Dispatchers.IO) {
                            val viewportWidth = with(density) { 320.dp.toPx() }
                            val viewportHeight = viewportWidth / LEDGER_HERO_ASPECT_RATIO
                            val baseScale = minOf(viewportWidth / bitmap.width, viewportHeight / bitmap.height)
                            val effectiveScale = (baseScale * scale).coerceAtLeast(0.0001f)
                            val requestedCropWidth = viewportWidth / effectiveScale
                            val requestedCropHeight = requestedCropWidth / LEDGER_HERO_ASPECT_RATIO
                            val sourceFitScale = minOf(
                                bitmap.width / requestedCropWidth,
                                bitmap.height / requestedCropHeight,
                            ).coerceAtMost(1f)
                            val cropWidth = requestedCropWidth * sourceFitScale
                            val cropHeight = requestedCropHeight * sourceFitScale
                            val centerX = bitmap.width / 2f - offsetX / effectiveScale
                            val centerY = bitmap.height / 2f - offsetY / effectiveScale
                            val left = (centerX - cropWidth / 2f).coerceIn(0f, bitmap.width - cropWidth)
                            val top = (centerY - cropHeight / 2f).coerceIn(0f, bitmap.height - cropHeight)
                            val cropped = Bitmap.createBitmap(bitmap, left.toInt(), top.toInt(), cropWidth.toInt().coerceAtLeast(1), cropHeight.toInt().coerceAtLeast(1))
                            val directory = File(context.filesDir, "ledger_covers").apply { mkdirs() }
                            val file = File(directory, "cover_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                            "$CROPPED_LEDGER_COVER_PREFIX${file.absolutePath}"
                        }
                        onConfirm(key)
                    }
                },
                Modifier.weight(1f),
                enabled = sourceBitmap != null,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) { Text("使用此裁剪") }
        }
    }
}

/** 在账本编辑页底部提供删除与保存操作。 */
@Composable
private fun LedgerEditorBottomBar(
    writeInProgress: Boolean,
    saveEnabled: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onDelete, Modifier.weight(1f), enabled = !writeInProgress) { Text("删除") }
        Button(onSave, Modifier.weight(1f), enabled = saveEnabled, colors = ButtonDefaults.buttonColorsPrimary()) { Text("保存") }
    }
}

/** 多选一个账户可在哪些账本中使用。 */
@Composable
fun AccountLedgerPickerScreen(
    ledgers: List<LedgerRecord>,
    selectedIds: Set<Long>,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
) {
    SecondaryScaffold(
        title = "适用账本",
        backdrop = backdrop,
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = innerPadding.calculateTopPadding())) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), insideMargin = PaddingValues(0.dp)) {
                    ledgers.forEach { ledger ->
                        SwitchPreference(
                            checked = ledger.id in selectedIds,
                            onCheckedChange = { checked ->
                                onSelectionChange(
                                    if (checked) selectedIds + ledger.id else selectedIds - ledger.id,
                                )
                            },
                            title = ledger.name,
                            summary = if (ledger.isHidden) "已隐藏" else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
        }
    }
}
