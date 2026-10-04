package com.vos.accounting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vos.accounting.data.LedgerRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sign
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 返回持久化账本动画标识对应的名称。 */
internal fun ledgerAnimationTitle(animation: String): String = if (animation == "flip") "翻转换层" else "轻盈叠层"

/** 展示并排动态动画选项，点击后立即保存选择。 */
@Composable
internal fun LedgerAnimationScreen(
    selectedAnimation: String,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    SecondaryScaffold(title = "账本切换动画", backdrop = backdrop, onBack = onBack) { padding ->
        SecondaryList(innerPadding = padding) {
            item {
                Text(
                    "切换效果",
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            item {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("stack", "flip").forEach { animation ->
                        val selected = animation == selectedAnimation
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(
                                modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
                                cornerRadius = 22.dp,
                                insideMargin = PaddingValues(3.dp),
                                colors = CardDefaults.defaultColors(
                                    color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer,
                                ),
                                onClick = { onSelect(animation) },
                            ) {
                                LedgerAnimationPreview(animation)
                            }
                            Text(
                                ledgerAnimationTitle(animation),
                                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
                                    .clickable(role = Role.RadioButton) { onSelect(animation) },
                                style = MiuixTheme.textStyles.body1,
                                color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), insideMargin = PaddingValues(20.dp)) {
                    Text("上下轻划切换账本", style = MiuixTheme.textStyles.body1)
                    Text(
                        "向上或向下拖动超过展开阈值，即可展开全部账本；点击卡片选中并收起。",
                        Modifier.padding(top = 8.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            item {
                Text(
                    "预览自动播放，点击选择后立即生效",
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 在缩小的首页中运行真实切换组件，示意账本只存在于预览内存。 */
@Composable
private fun LedgerAnimationPreview(animation: String) {
    val ledgers = remember {
        listOf("日常账本" to "cover_ocean", "旅行账本" to "cover_sunset", "家庭账本" to "cover_mint")
            .mapIndexed { index, (name, cover) ->
                LedgerRecord(index.toLong() + 1, name, cover, true, "cny", false, index, 0)
            }
    }
    val summaries = remember { ledgers.associate { it.id to LedgerAssetSummary(0, 0, 0, "¥") } }
    var currentId by remember { mutableLongStateOf(1L) }
    Box(
        Modifier.fillMaxWidth().squircleClip(19.dp)
            .background(MiuixTheme.colorScheme.surface)
            .layout { measurable, constraints ->
                val designWidth = 360.dp.roundToPx()
                val scale = constraints.maxWidth.toFloat() / designWidth
                val placeable = measurable.measure(Constraints.fixed(designWidth, 610.dp.roundToPx()))
                layout(constraints.maxWidth, (placeable.height * scale).toInt()) {
                    placeable.placeWithLayer(0, 0) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            },
    ) {
        LedgerExpansionHost(ledgers, summaries, { currentId = it }, interactive = false) { expansion ->
            Column(Modifier.fillMaxSize()) {
                Text("9:41", Modifier.padding(20.dp), style = MiuixTheme.textStyles.body2)
                Text("首页", Modifier.padding(horizontal = 20.dp).graphicsLayer { alpha = 1f - expansion.titleOcclusion }, style = MiuixTheme.textStyles.title1)
                Spacer(Modifier.height(94.dp))
                HomeLedgerStack(ledgers, currentId, summaries, { currentId = it }, animation, preview = true, expansion = expansion)
                Card(Modifier.padding(12.dp).fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
                    Text("动画预览", style = MiuixTheme.textStyles.body1)
                    Text("上下切换账本", Modifier.padding(top = 12.dp), style = MiuixTheme.textStyles.body2)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp).width(100.dp).height(4.dp)
                    .background(MiuixTheme.colorScheme.onSurfaceVariantSummary))
            }
        }
    }
}

/** 描述整张卡片在切换或展开衔接时的几何状态。 */
internal data class LedgerCardMotion(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val alpha: Float = 1f,
    val rotationX: Float = 0f,
    val rotationZ: Float = 0f,
    val zIndex: Float = 0f,
    val contentAlpha: Float = 1f,
)

/** 获取未被分页视口裁剪的真实边界，离屏时仍保留有效尺寸。 */
internal fun LayoutCoordinates.ledgerMotionBounds(): Rect =
    findRootCoordinates().localBoundingBoxOf(this, clipBounds = false)

/** 按卡片高度归一化的完整展开拖动距离。 */
internal const val LEDGER_EXPAND_DRAG_DISTANCE = 2.1f

/** 将手指距离映射到抽出半程，阻力随距离增长而增大。 */
internal fun ledgerDragProgress(distance: Float): Float {
    val travel = 0.6f * abs(distance) / (1f + 0.15f * abs(distance))
    return sign(distance) * asin(travel.coerceAtMost(0.98f)) / PI.toFloat()
}

/** 将手指速度换算成当前阻尼曲线上的动画速度。 */
internal fun ledgerDragProgressSlope(distance: Float): Float {
    val resistance = 1f + 0.15f * abs(distance)
    val travel = 0.6f * abs(distance) / resistance
    return if (travel >= 0.98f) 0f else 0.6f / (resistance * resistance * PI.toFloat() * sqrt(1f - travel * travel))
}

/** 保存展开手势与吸附进度，使拖动、取消和再次接管保持连续。 */
internal class LedgerExpansionState {
    var sourceBounds by mutableStateOf(Rect.Zero)
    var upwardTravelPx by mutableFloatStateOf(0f)
    var switchOcclusion by mutableFloatStateOf(0f)
    val titleOcclusion: Float get() = if (animation != "flip") 0f else if (visible) {
        if (closingToHome) value else openingOcclusion + (1f - openingOcclusion) * value
    } else switchOcclusion
    var visible by mutableStateOf(false)
    var selectedId by mutableLongStateOf(0L)
    var sourceId by mutableLongStateOf(0L)
    var switchTargetId by mutableStateOf<Long?>(null)
    var direction by mutableStateOf(1)
    var restingDirection by mutableStateOf(1)
    var animation by mutableStateOf("stack")
    var closingToHome by mutableStateOf(false)
    var switchProgress by mutableFloatStateOf(0f)
    var openingMotions by mutableStateOf<Map<Long, LedgerCardMotion>>(emptyMap())
    private var openingOcclusion by mutableFloatStateOf(0f)
    var dragProgress by mutableFloatStateOf(0f)
    var dragging by mutableStateOf(false)
    val progress = Animatable(0f)
    private var job: Job? = null
    val value: Float get() = if (dragging) dragProgress else progress.value

    /** 离开首页时取消未完成的展开与选择回调，恢复单卡状态。 */
    fun dismiss() {
        job?.cancel()
        job = null
        visible = false
        dragProgress = 0f
        dragging = true
        switchOcclusion = 0f
        openingMotions = emptyMap()
    }

    /** 从当前动画位置接管下拉手势并固定来源账本。 */
    fun begin(
        ledgerId: Long,
        targetId: Long? = null,
        dragDirection: Int = 1,
        effect: String = "stack",
        handoffProgress: Float = 0f,
        motions: Map<Long, LedgerCardMotion> = emptyMap(),
    ) {
        val current = if (visible) value else 0f
        job?.cancel()
        dragProgress = current
        selectedId = ledgerId
        sourceId = ledgerId
        switchTargetId = targetId
        direction = dragDirection
        animation = effect
        switchProgress = handoffProgress
        openingMotions = motions
        openingOcclusion = switchOcclusion
        closingToHome = false
        dragging = true
        visible = true
    }

    /** 将展开或收起吸附到端点，收起后再提交账本选择。 */
    fun settle(scope: CoroutineScope, target: Float, onClosed: () -> Unit = {}) {
        val current = value
        job?.cancel()
        job = scope.launch {
            progress.snapTo(current)
            dragging = false
            progress.animateTo(
                target,
                if (animation == "flip") folmeSpring(damping = 1f, response = 0.45f, visibilityThreshold = 0.001f)
                else tween(320, easing = FastOutSlowInEasing),
            )
            if (target == 0f) {
                visible = false
                onClosed()
            }
        }
    }
}

/** 在页面内容区承载原位展开层，原列表保留布局和滚动状态。 */
@Composable
internal fun LedgerExpansionHost(
    ledgers: List<LedgerRecord>,
    summaries: Map<Long, LedgerAssetSummary>,
    onSelect: (Long) -> Unit,
    interactive: Boolean = true,
    active: Boolean = true,
    onTitleOcclusionChange: (Float) -> Unit = {},
    content: @Composable (LedgerExpansionState) -> Unit,
) {
    val state = remember { LedgerExpansionState() }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val titleOcclusion = state.titleOcclusion
    SideEffect { onTitleOcclusionChange(titleOcclusion) }
    LaunchedEffect(active) { if (!active) state.dismiss() }
    DisposableEffect(Unit) {
        onDispose {
            state.dismiss()
            onTitleOcclusionChange(0f)
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { bounds = it.ledgerMotionBounds() }) {
        content(state)
        if (active && state.visible) {
            LedgerExpansion(ledgers, summaries, state, bounds, onSelect, interactive)
        }
    }
}

/** 将整张账本卡片随下拉进度铺开，遮罩和卡片共享可逆进度。 */
@Composable
private fun LedgerExpansion(
    ledgers: List<LedgerRecord>,
    summaries: Map<Long, LedgerAssetSummary>,
    state: LedgerExpansionState,
    hostBounds: Rect,
    onSelect: (Long) -> Unit,
    interactive: Boolean,
) {
    val scope = rememberCoroutineScope()
    val close: (Long) -> Unit = { id ->
        state.selectedId = id
        state.closingToHome = true
        state.settle(scope, 0f) { onSelect(id) }
    }
    BackHandler(enabled = interactive) { close(state.selectedId) }
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val source = state.sourceBounds
        val hostScale = constraints.maxWidth / hostBounds.width
        val left = with(density) { ((source.left - hostBounds.left) * hostScale).toDp() } + 12.dp
        val width = with(density) { (source.width * hostScale).toDp() } - 24.dp
        val sourceTop = with(density) { ((source.top - hostBounds.top) * hostScale).toDp() } + 8.dp
        val cardHeight = width / LEDGER_HERO_ASPECT_RATIO
        val top = if (state.animation == "flip") {
            maxOf(
                insets.calculateTopPadding() + 16.dp,
                (maxHeight - insets.calculateBottomPadding() - (cardHeight + 20.dp) * ledgers.size - 40.dp) / 2,
            )
        } else maxOf(24.dp, minOf(sourceTop, maxHeight * 0.22f))
        val flipMotions = if (state.animation == "flip") {
            if (state.closingToHome || state.openingMotions.isEmpty()) {
                ledgerFlipDeckMotions(
                    ledgers, state.selectedId, null, 0f, state.direction, state.restingDirection,
                    with(density) { cardHeight.toPx() }, density.density, state.upwardTravelPx,
                )
            } else state.openingMotions
        } else emptyMap()
        Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, enabled = interactive) { close(state.selectedId) })
        Column(
            Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, enabled = interactive) { close(state.selectedId) }
                .verticalScroll(scroll, enabled = interactive && state.value == 1f)
                .padding(top = top, bottom = insets.calculateBottomPadding() + 40.dp),
        ) {
            ledgers.forEachIndexed { index, ledger ->
                val depth = ((ledgers.indexOfFirst { it.id == state.selectedId } - ledgers.indexOfFirst { it.id == ledger.id }) * state.restingDirection).mod(ledgers.size)
                val selected = depth == 0
                val targetY = top + (cardHeight + 20.dp) * index
                val handoff = state.switchTargetId != null && !state.closingToHome
                val switchingCard = handoff && (ledger.id == state.sourceId || ledger.id == state.switchTargetId)
                val collapsedMotion = if (state.animation == "flip") {
                    flipMotions.getValue(ledger.id)
                } else if (switchingCard) {
                    ledgerSwitchMotion(
                        state.switchProgress, state.direction, state.animation,
                        ledger.id == state.switchTargetId, with(density) { cardHeight.toPx() }, density.density, state.upwardTravelPx,
                    )
                } else {
                    LedgerCardMotion(
                        scale = 1f - 0.014f * minOf(depth, 2),
                        translationY = with(density) { (-4.dp * minOf(depth, 2)).toPx() },
                        alpha = when {
                            depth == 0 || (state.animation == "flip" && depth <= 2) -> 1f
                            depth == 1 -> 0.36f
                            depth == 2 -> 0.18f
                            else -> 0f
                        },
                    )
                }
                val layer = if (state.animation == "flip") collapsedMotion.zIndex else if (switchingCard) {
                    if (ledger.id == state.sourceId) 3f else 2f
                } else (ledgers.size - depth).toFloat() / ledgers.size
                LedgerHeroCard(
                    modifier = Modifier.padding(start = left).width(width)
                        .padding(bottom = 20.dp).zIndex(layer)
                        .graphicsLayer {
                            val p = state.value
                            translationX = collapsedMotion.translationX * (1f - p)
                            translationY = ((sourceTop - targetY).toPx() + collapsedMotion.translationY + scroll.value) * (1f - p)
                            scaleX = collapsedMotion.scale + (1f - collapsedMotion.scale) * p
                            scaleY = scaleX
                            rotationX = collapsedMotion.rotationX * (1f - p)
                            rotationZ = collapsedMotion.rotationZ * (1f - p)
                            cameraDistance = cardHeight.toPx() * 8f
                            alpha = collapsedMotion.alpha + (1f - collapsedMotion.alpha) * p
                        }
                        .squircleClip(16.dp)
                        .clickable(enabled = interactive && state.value == 1f) { close(ledger.id) },
                    ledger = ledger,
                    summary = summaries.getValue(ledger.id),
                    contentAlpha = if (state.animation == "flip") {
                        collapsedMotion.contentAlpha + (1f - collapsedMotion.contentAlpha) * state.value
                    } else if (selected || switchingCard) 1f else state.value,
                    depthDimAlpha = if (state.animation == "flip" || selected || switchingCard) 0f else 0.42f * (1f - state.value),
                )
            }
            Text(
                "点击账本切换 · 点击空白收起",
                Modifier.padding(start = left).width(width).graphicsLayer { alpha = state.value },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

/** 用真实卡片的遮挡和换层呈现抽出、分离、覆盖与归位。 */
@Composable
internal fun LedgerFlipDeck(
    ledgers: List<LedgerRecord>,
    sourceId: Long,
    targetId: Long?,
    summaries: Map<Long, LedgerAssetSummary>,
    progress: Float,
    direction: Int,
    restingDirection: Int,
    cardHeightPx: Float,
    upwardTravelPx: Float,
    modifier: Modifier,
    reveal: Float = 0f,
) {
    val density = LocalDensity.current.density
    val motions = ledgerFlipDeckMotions(
        ledgers, sourceId, targetId, progress, direction, restingDirection, cardHeightPx, density, upwardTravelPx, reveal,
    )
    ledgers.forEach { ledger ->
        val motion = motions.getValue(ledger.id)
        if (motion.alpha > 0f) {
            LedgerFlipCard(ledger, summaries.getValue(ledger.id), motion, cardHeightPx, modifier)
        }
    }
}

/** 为换层与展开共用同一份完整卡片姿态，包含尚未露出的账本。 */
internal fun ledgerFlipDeckMotions(
    ledgers: List<LedgerRecord>,
    sourceId: Long,
    targetId: Long?,
    progress: Float,
    direction: Int,
    restingDirection: Int,
    cardHeightPx: Float,
    density: Float,
    upwardTravelPx: Float,
    reveal: Float = if (targetId == null) 0f else 1f,
): Map<Long, LedgerCardMotion> {
    val motions = ledgers.associate { ledger ->
        ledger.id to LedgerCardMotion(alpha = 0f, contentAlpha = 0f)
    }.toMutableMap()
    val sourceIndex = ledgers.indexOfFirst { it.id == sourceId }
    val far = if (targetId != null) {
        ledgers[(sourceIndex + direction).mod(ledgers.size)]
    } else ledgers[(sourceIndex - restingDirection * 2).mod(ledgers.size)]
    if (ledgers.size > 2 && far.id != sourceId && far.id != targetId) {
        motions[far.id] = ledgerFanMotion(2, reveal, density)
    }
    if (targetId == null && ledgers.size > 1) {
        val near = ledgers[(sourceIndex - restingDirection).mod(ledgers.size)]
        motions[near.id] = ledgerFanMotion(1, reveal, density)
    }
    motions[sourceId] = ledgerSwitchMotion(
        if (targetId == null) 0f else progress, direction, "flip", false, cardHeightPx, density, upwardTravelPx,
    )
    if (targetId != null) {
        val fan = ledgerFanMotion(1, reveal, density)
        val motion = ledgerSwitchMotion(progress, direction, "flip", true, cardHeightPx, density, upwardTravelPx)
        val fanWeight = (1f - progress * 2f).coerceAtLeast(0f)
        motions[targetId] = motion.copy(
            translationX = fan.translationX * fanWeight,
            translationY = motion.translationY + 4f * density * (1f - progress) * fanWeight,
            rotationZ = motion.rotationZ + fan.rotationZ * fanWeight,
            alpha = maxOf(reveal, (progress * 6f).coerceAtMost(1f)),
        )
        val outgoing = motions.getValue(sourceId)
        val returnWeight = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
        motions[sourceId] = outgoing.copy(
            translationX = fan.translationX * returnWeight,
            translationY = outgoing.translationY + 4f * density * returnWeight,
            rotationZ = outgoing.rotationZ + fan.rotationZ * returnWeight,
        )
    }
    return motions
}

/** 触摸时向两侧小幅错开背景卡片，松手后完全藏回前景轮廓。 */
internal fun ledgerFanMotion(depth: Int, reveal: Float, density: Float): LedgerCardMotion = LedgerCardMotion(
    scale = 1f - 0.014f * reveal,
    translationX = (if (depth == 1) -3f else 3f) * density * reveal,
    rotationZ = (if (depth == 1) -1.8f else 1.8f) * reveal,
    alpha = reveal,
    zIndex = if (depth == 1) 2f else 1f,
    contentAlpha = 0f,
)

/** 将完整卡片的外壳、封面与文字按统一姿态绘制。 */
@Composable
private fun LedgerFlipCard(
    ledger: LedgerRecord,
    summary: LedgerAssetSummary,
    motion: LedgerCardMotion,
    cardHeightPx: Float,
    modifier: Modifier,
) {
    LedgerHeroCard(
        modifier = modifier.zIndex(motion.zIndex).graphicsLayer {
            scaleX = motion.scale
            scaleY = motion.scale
            translationX = motion.translationX
            translationY = motion.translationY
            rotationX = motion.rotationX
            rotationZ = motion.rotationZ
            cameraDistance = cardHeightPx * 8f
            alpha = motion.alpha
        },
        ledger = ledger,
        summary = summary,
        contentAlpha = motion.contentAlpha,
    )
}
