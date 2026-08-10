package com.vos.accounting.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.LedgerRecord
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.data.convertCurrencyMinor
import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransferDirection
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.window.WindowListPopup
import kotlin.math.absoluteValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 带组头列表 Card 的组头最小高度。 */
internal val GROUPED_CARD_HEADER_MIN_HEIGHT = 40.dp

/** 带组头列表 Card 的水平内容内边距。 */
internal val GROUPED_CARD_HORIZONTAL_PADDING = 16.dp

/** 带组头列表 Card 内容行的垂直内边距。 */
internal val GROUPED_CARD_ROW_VERTICAL_PADDING = 12.dp

/** 带组头列表 Card 内容行的图标容器尺寸。 */
internal val GROUPED_CARD_ICON_CONTAINER_SIZE = 40.dp

/** 带组头列表 Card 内容行的图标尺寸。 */
internal val GROUPED_CARD_ICON_SIZE = 24.dp

/** 带组头列表 Card 内容行图标容器的圆角尺寸。 */
internal val GROUPED_CARD_ICON_CORNER_SIZE = 12.dp

/** 首页、明细和账户详细页 Hero Card 的统一宽高比。 */
internal const val LEDGER_HERO_ASPECT_RATIO = 21f / 9f

/** 顶部数据 Hero Card 的统一内容内边距。 */
internal val HERO_CARD_CONTENT_PADDING = 20.dp

/** 支出金额启用颜色显示时使用的红色。 */
internal val EXPENSE_AMOUNT_COLOR = Color(0xFFE5484D)

/** 收入金额启用颜色显示时使用的绿色。 */
internal val INCOME_AMOUNT_COLOR = Color(0xFF2E9B55)

/** 按设置返回普通收支金额颜色，转账与币种兑换保持原色。 */
internal fun transactionAmountColor(type: TransactionType, enabled: Boolean, defaultColor: Color): Color = when {
    !enabled -> defaultColor
    type == TransactionType.EXPENSE -> EXPENSE_AMOUNT_COLOR
    type == TransactionType.INCOME -> INCOME_AMOUNT_COLOR
    else -> defaultColor
}

/** 账本叠放中第一层背景卡片的缩放比例。 */
private const val LEDGER_STACK_NEAR_BACKGROUND_SCALE = 0.986f

/** 账本叠放中第二层背景卡片的缩放比例。 */
private const val LEDGER_STACK_FAR_BACKGROUND_SCALE = 0.972f

/** 首页账本叠层中为前景 Hero Card 预留的顶部空间。 */
private val LEDGER_STACK_FOREGROUND_TOP_INSET = 8.dp

/** 账本叠放卡片释放后触发切换的拖动距离阈值，按卡片高度比例计。 */
private const val LEDGER_STACK_SWITCH_THRESHOLD = 0.35f

/** 账本叠放卡片快速甩动触发切换的速度阈值。 */
private val LEDGER_STACK_FLING_VELOCITY = 400.dp

/** 入场账本卡片相对主卡的初始缩放比例，与近层背景卡片保持一致。 */
private const val LEDGER_STACK_INCOMING_SCALE = 0.986f

/** 退场账本卡片相对主卡的最大缩放比例。 */
private const val LEDGER_STACK_OUTGOING_SCALE = 1.015f

/** 入场账本卡片从相反方向进入时的最大纵向位移。 */
private val LEDGER_STACK_INCOMING_OFFSET = 4.dp

/** 退场账本卡片顺手势退层时的最大纵向位移。 */
private val LEDGER_STACK_OUTGOING_OFFSET = 6.dp

/** 账本叠放动画在裁剪视口内可使用的底部运动空间。 */
private val LEDGER_STACK_MOTION_VIEWPORT_BOTTOM = 12.dp

/** 账本叠放中近层背景卡片的最大透明度。 */
private const val LEDGER_STACK_NEAR_BACKGROUND_ALPHA = 0.36f

/** 账本叠放中远层背景卡片的最大透明度。 */
private const val LEDGER_STACK_FAR_BACKGROUND_ALPHA = 0.18f

/** 账本叠放中近层背景卡片相对远层的顶部层叠距离。 */
private val LEDGER_STACK_BACKGROUND_OFFSET = 4.dp

/** 预热远层背景卡片的初始缩放比例。 */
private const val LEDGER_STACK_PREWARM_BACKGROUND_SCALE = 0.966f

/** 背景账本卡片为避免展示可读文字而叠加的遮罩透明度。 */
private const val LEDGER_STACK_BACKGROUND_DIM_ALPHA = 0.42f

/** 静止时背景账本卡片的默认切换方向，正值表示向下切到前一账本。 */
private const val LEDGER_STACK_DEFAULT_DIRECTION = 1

/** 单个账本封面页面需要展示的资产汇总数据。 */
private data class LedgerAssetSummary(
    val netAssets: Long,
    val totalAssets: Long,
    val totalLiabilities: Long,
    val currencySymbol: String,
)

/** 表示账本卡片手势所处的稳定、拖动或吸附阶段。 */
private enum class LedgerStackPhase {
    IDLE,
    DRAGGING,
    SETTLING,
}

/** 固定一次账本切换的来源、目标和手势方向，避免中途重算相邻账本。 */
private data class LedgerStackTransition(
    val fromLedgerId: Long,
    val toLedgerId: Long,
    val direction: Int,
)

/**
 * 抵消账本叠层内部的顶部预留，使首页前景 Hero Card 与其他主分页的首张 Card 对齐，
 * 同时保留卡片栈完整的裁剪视口和底部间距。
 */
private fun Modifier.alignLedgerStackForegroundWithHero(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val topInsetPx = LEDGER_STACK_FOREGROUND_TOP_INSET.roundToPx()
    layout(
        width = placeable.width,
        height = placeable.height - topInsetPx,
    ) {
        placeable.placeRelative(x = 0, y = -topInsetPx)
    }
}

/**
 * 展示余额、快捷记账入口与最近账目。
 */
@Composable
fun HomeScreen(
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
    onOpenAccount: (Long) -> Unit,
    onSelectLedger: (Long) -> Unit,
) {
    val activeAccounts = uiState.accounts.filterNot(AccountEntity::isArchived)
    val accountBalances = remember(activeAccounts, uiState.accountBalances) {
        activeAccounts.associateWith { account ->
            uiState.accountBalances[account.id]?.balanceMinor ?: account.openingBalanceMinor
        }
    }
    val currencies = remember(uiState.currencies) {
        uiState.currencies.associateBy(CurrencyEntity::key)
    }
    val availableLedgers = remember(uiState.ledgers) {
        uiState.ledgers.filterNot { it.isHidden }.ifEmpty { uiState.ledgers }
    }
    if (availableLedgers.isEmpty()) {
        MainTabList(innerPadding = innerPadding) {
            item { EmptyCard(text = "暂无账本") }
        }
        return
    }
    val ledger = availableLedgers.firstOrNull { it.id == uiState.currentLedgerId }
        ?: availableLedgers.first()
    val hasMissingCurrency = (availableLedgers.map(LedgerRecord::baseCurrencyKey) + activeAccounts.map(AccountEntity::currencyKey))
        .any { currencies[it] == null }
    if (hasMissingCurrency) {
        MainTabList(innerPadding = innerPadding) {
            item { EmptyCard(text = "币种数据异常，请恢复相关币种后重试") }
        }
        return
    }
    val accountIdsByLedger = remember(uiState.accountLedgerCrossRefs) {
        uiState.accountLedgerCrossRefs.groupBy { it.ledgerId }
            .mapValues { (_, refs) -> refs.mapTo(mutableSetOf()) { it.accountId } }
    }
    val ledgerSummaries = remember(availableLedgers, accountBalances, currencies, accountIdsByLedger) {
        availableLedgers.associate { visibleLedger ->
            val baseCurrency = currencies.getValue(visibleLedger.baseCurrencyKey)
            val convertedBalances = accountBalances
                .filterKeys { it.id in accountIdsByLedger[visibleLedger.id].orEmpty() }
                .mapValues { (account, balance) ->
                    convertCurrencyMinor(
                        balance,
                        currencies.getValue(account.currencyKey).rateToCnyScaled,
                        baseCurrency.rateToCnyScaled,
                    )
                }
            val totalAssets = convertedBalances.values.sumOf { maxOf(it, 0L) }
            val totalLiabilities = convertedBalances.values.sumOf { -minOf(it, 0L) }
            visibleLedger.id to LedgerAssetSummary(
                netAssets = totalAssets - totalLiabilities,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                currencySymbol = baseCurrency.symbol,
            )
        }
    }
    val baseCurrency = currencies.getValue(ledger.baseCurrencyKey)
    val cnyBalances = remember(accountBalances, currencies, baseCurrency) {
        accountBalances.mapValues { (account, balance) ->
            convertCurrencyMinor(balance, currencies.getValue(account.currencyKey).rateToCnyScaled, baseCurrency.rateToCnyScaled)
        }
    }
    var displayedLedgerId by remember { mutableStateOf(ledger.id) }
    var ledgerSelectionPending by remember { mutableStateOf(false) }
    LaunchedEffect(ledger.id) {
        if (!ledgerSelectionPending || ledger.id == displayedLedgerId) {
            displayedLedgerId = ledger.id
            ledgerSelectionPending = false
        }
    }
    val selectDisplayedLedger: (Long) -> Unit = { ledgerId ->
        if (ledgerId != displayedLedgerId) {
            displayedLedgerId = ledgerId
            ledgerSelectionPending = true
            onSelectLedger(ledgerId)
        }
    }
    val displayedAccounts = activeAccounts.filter {
        it.id in accountIdsByLedger[displayedLedgerId].orEmpty()
    }

    MainTabList(innerPadding = innerPadding) {
        item {
            HomeLedgerStack(
                ledgers = availableLedgers,
                currentLedgerId = displayedLedgerId,
                ledgerSummaries = ledgerSummaries,
                onSelectLedger = selectDisplayedLedger,
            )
        }
        if (displayedAccounts.isEmpty()) {
            item { EmptyCard(text = "当前账本暂无账户") }
        } else {
            uiState.accountTypes.forEach { type ->
                val accounts = displayedAccounts.filter { it.typeKey == type.key }
                if (accounts.isNotEmpty()) {
                    item(key = type.key) {
                        HomeAccountGroup(
                            modifier = Modifier.animateItem(),
                            type = type,
                            accounts = accounts,
                            balances = accountBalances,
                            cnyBalances = cnyBalances,
                            currencies = currencies,
                            baseCurrency = baseCurrency,
                            onOpenAccount = onOpenAccount,
                        )
                    }
                }
            }
        }
    }
}

/** 根据来源账本和拖动方向固定一组相邻账本，确保手势中途不会重算目标。 */
private fun ledgerStackTransition(
    ledgers: List<LedgerRecord>,
    sourceLedgerId: Long,
    direction: Int,
): LedgerStackTransition? {
    if (ledgers.size < 2 || direction == 0) return null
    val sourceIndex = ledgers.indexOfFirst { it.id == sourceLedgerId }
    if (sourceIndex < 0) return null
    return LedgerStackTransition(
        fromLedgerId = sourceLedgerId,
        toLedgerId = ledgers[(sourceIndex - direction).mod(ledgers.size)].id,
        direction = direction,
    )
}

/** 通过完整账本卡片的小幅换层、缩放和叠化切换相邻账本。 */
@Composable
private fun HomeLedgerStack(
    ledgers: List<LedgerRecord>,
    currentLedgerId: Long,
    ledgerSummaries: Map<Long, LedgerAssetSummary>,
    onSelectLedger: (Long) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val settleProgress = remember { Animatable(0f) }
    var phase by remember { mutableStateOf(LedgerStackPhase.IDLE) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var settledLedgerId by remember { mutableLongStateOf(currentLedgerId) }
    var optimisticLedgerId by remember { mutableStateOf<Long?>(null) }
    var transition by remember { mutableStateOf<LedgerStackTransition?>(null) }
    var motionEpoch by remember { mutableIntStateOf(0) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val latestLedgers by rememberUpdatedState(ledgers)
    val latestOnSelectLedger by rememberUpdatedState(onSelectLedger)
    val multipleLedgers = ledgers.size > 1

    LaunchedEffect(currentLedgerId, ledgers, phase) {
        if (optimisticLedgerId == currentLedgerId) {
            optimisticLedgerId = null
        } else if (phase == LedgerStackPhase.IDLE && optimisticLedgerId == null && ledgers.any { it.id == currentLedgerId }) {
            settledLedgerId = currentLedgerId
        }
    }

    val settleTo: (Float, Float, Float, LedgerStackTransition?, Int) -> Unit =
        { releasedProgress, targetProgress, initialVelocity, transitionAtRelease, epoch ->
            settleJob = scope.launch {
                try {
                    settleProgress.snapTo(releasedProgress)
                    phase = LedgerStackPhase.SETTLING
                    settleProgress.animateTo(
                        targetValue = targetProgress,
                        animationSpec = tween(
                            durationMillis = if (targetProgress == 0f) 180 else 220,
                            easing = FastOutSlowInEasing,
                        ),
                        initialVelocity = initialVelocity,
                    )
                    if (epoch != motionEpoch) return@launch
                    val targetLedgerId = if (targetProgress == 0f) null else transitionAtRelease?.toLedgerId
                    if (targetLedgerId != null) {
                        settledLedgerId = targetLedgerId
                        optimisticLedgerId = targetLedgerId
                        latestOnSelectLedger(targetLedgerId)
                    }
                    settleProgress.snapTo(0f)
                    transition = null
                    phase = LedgerStackPhase.IDLE
                } finally {
                    if (epoch == motionEpoch) {
                        settleJob = null
                    }
                }
            }
        }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (multipleLedgers) {
                    Modifier.alignLedgerStackForegroundWithHero()
                } else {
                    Modifier
                },
            ),
    ) {
        val horizontalPaddingPx = with(density) { 12.dp.toPx() }
        val cardWidthPx = constraints.maxWidth - horizontalPaddingPx * 2f
        val cardHeightPx = cardWidthPx / LEDGER_HERO_ASPECT_RATIO
        val peekPx = with(density) { LEDGER_STACK_FOREGROUND_TOP_INSET.toPx() }
        val incomingOffsetPx = with(density) { LEDGER_STACK_INCOMING_OFFSET.toPx() }
        val outgoingOffsetPx = with(density) { LEDGER_STACK_OUTGOING_OFFSET.toPx() }
        val backgroundOffsetPx = with(density) { LEDGER_STACK_BACKGROUND_OFFSET.toPx() }
        val motionViewportBottomPx = with(density) { LEDGER_STACK_MOTION_VIEWPORT_BOTTOM.toPx() }
        val stackHeightPx = cardHeightPx + motionViewportBottomPx + if (multipleLedgers) peekPx else 0f
        val flingVelocityPx = with(density) { LEDGER_STACK_FLING_VELOCITY.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { stackHeightPx.toDp() })
                .clipToBounds()
                .pointerInput(cardHeightPx, multipleLedgers, flingVelocityPx) {
                    if (!multipleLedgers) return@pointerInput
                    val velocityTracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            motionEpoch += 1
                            settleJob?.cancel()
                            settleJob = null
                            dragProgress = settleProgress.value
                            phase = LedgerStackPhase.DRAGGING
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val nextProgress = (dragProgress + dragAmount / cardHeightPx).coerceIn(-1f, 1f)
                            val direction = when {
                                nextProgress > 0f -> 1
                                nextProgress < 0f -> -1
                                else -> 0
                            }
                            if (direction != 0) {
                                val currentTransition = transition
                                if (
                                    currentTransition == null ||
                                    currentTransition.fromLedgerId != settledLedgerId ||
                                    currentTransition.direction != direction
                                ) {
                                    transition = ledgerStackTransition(latestLedgers, settledLedgerId, direction)
                                }
                            }
                            dragProgress = nextProgress
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().y
                            val target = when {
                                dragProgress > LEDGER_STACK_SWITCH_THRESHOLD ||
                                    (dragProgress > 0f && velocity >= flingVelocityPx) -> 1f
                                dragProgress < -LEDGER_STACK_SWITCH_THRESHOLD ||
                                    (dragProgress < 0f && velocity <= -flingVelocityPx) -> -1f
                                else -> 0f
                            }
                            val direction = when {
                                target > 0f -> 1
                                target < 0f -> -1
                                dragProgress > 0f -> 1
                                dragProgress < 0f -> -1
                                else -> 0
                            }
                            val transitionAtRelease = if (direction == 0) {
                                transition
                            } else {
                                transition?.takeIf {
                                    it.fromLedgerId == settledLedgerId && it.direction == direction
                                } ?: ledgerStackTransition(latestLedgers, settledLedgerId, direction)
                            }
                            transition = transitionAtRelease
                            settleTo(
                                dragProgress,
                                target,
                                velocity / cardHeightPx,
                                transitionAtRelease,
                                motionEpoch,
                            )
                        },
                        onDragCancel = {
                            settleTo(
                                dragProgress,
                                0f,
                                0f,
                                transition,
                                motionEpoch,
                            )
                        },
                    )
                },
        ) {
            val signedProgress = when (phase) {
                LedgerStackPhase.DRAGGING -> dragProgress
                LedgerStackPhase.SETTLING -> settleProgress.value
                LedgerStackPhase.IDLE -> 0f
            }
            val activeTransition = transition
            val motionProgress = if (activeTransition == null) 0f else signedProgress.absoluteValue
            val currentLedger = ledgers.firstOrNull {
                it.id == (activeTransition?.fromLedgerId ?: settledLedgerId)
            } ?: ledgers.first()
            val incomingLedger = activeTransition
                ?.takeIf { motionProgress > 0f }
                ?.let { active -> ledgers.firstOrNull { it.id == active.toLedgerId } }
            val motionDirection = (activeTransition?.direction ?: LEDGER_STACK_DEFAULT_DIRECTION).toFloat()
            val backgroundTransition = ledgerStackTransition(
                ledgers,
                currentLedger.id,
                activeTransition?.direction ?: LEDGER_STACK_DEFAULT_DIRECTION,
            )
            val nearBackgroundLedger = backgroundTransition?.let { background ->
                ledgers.firstOrNull { it.id == background.toLedgerId }
            }
            val farBackgroundLedger = nearBackgroundLedger?.let { nearLedger ->
                ledgerStackTransition(
                    ledgers,
                    nearLedger.id,
                    activeTransition?.direction ?: LEDGER_STACK_DEFAULT_DIRECTION,
                )?.let { background -> ledgers.firstOrNull { it.id == background.toLedgerId } }
            }
            val targetBackgroundTransition = activeTransition?.let { active ->
                ledgerStackTransition(ledgers, active.toLedgerId, active.direction)
            }
            val promotingBackgroundLedger = targetBackgroundTransition?.let { background ->
                ledgers.firstOrNull { it.id == background.toLedgerId }
            }
            val replenishingBackgroundLedger = promotingBackgroundLedger
                ?.takeIf { ledgers.size > 2 }
                ?.let { promotingLedger ->
                    ledgerStackTransition(
                        ledgers,
                        promotingLedger.id,
                        motionDirection.toInt(),
                    )?.let { background -> ledgers.firstOrNull { it.id == background.toLedgerId } }
                }
            val backgroundHandoffTranslation = motionDirection * backgroundOffsetPx *
                motionProgress * (1f - motionProgress)

            if (multipleLedgers && activeTransition == null) {
                if (farBackgroundLedger != null && ledgers.size > 2) {
                    LedgerHeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .zIndex(0f)
                            .graphicsLayer {
                                scaleX = LEDGER_STACK_FAR_BACKGROUND_SCALE
                                scaleY = LEDGER_STACK_FAR_BACKGROUND_SCALE
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                alpha = LEDGER_STACK_FAR_BACKGROUND_ALPHA
                            },
                        ledger = farBackgroundLedger,
                        summary = ledgerSummaries.getValue(farBackgroundLedger.id),
                        contentAlpha = 0f,
                        depthDimAlpha = LEDGER_STACK_BACKGROUND_DIM_ALPHA,
                    )
                }
                if (nearBackgroundLedger != null) {
                    LedgerHeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = LEDGER_STACK_BACKGROUND_OFFSET)
                            .zIndex(1f)
                            .graphicsLayer {
                                scaleX = LEDGER_STACK_NEAR_BACKGROUND_SCALE
                                scaleY = LEDGER_STACK_NEAR_BACKGROUND_SCALE
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                alpha = LEDGER_STACK_NEAR_BACKGROUND_ALPHA
                            },
                        ledger = nearBackgroundLedger,
                        summary = ledgerSummaries.getValue(nearBackgroundLedger.id),
                        contentAlpha = 0f,
                        depthDimAlpha = LEDGER_STACK_BACKGROUND_DIM_ALPHA,
                    )
                }
            } else if (multipleLedgers) {
                if (replenishingBackgroundLedger != null) {
                    LedgerHeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .zIndex(0f)
                            .graphicsLayer {
                                scaleX = LEDGER_STACK_PREWARM_BACKGROUND_SCALE +
                                    (LEDGER_STACK_FAR_BACKGROUND_SCALE -
                                        LEDGER_STACK_PREWARM_BACKGROUND_SCALE) * motionProgress
                                scaleY = LEDGER_STACK_PREWARM_BACKGROUND_SCALE +
                                    (LEDGER_STACK_FAR_BACKGROUND_SCALE -
                                        LEDGER_STACK_PREWARM_BACKGROUND_SCALE) * motionProgress
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                translationY = backgroundHandoffTranslation * 0.5f
                                alpha = LEDGER_STACK_FAR_BACKGROUND_ALPHA * motionProgress
                            },
                        ledger = replenishingBackgroundLedger,
                        summary = ledgerSummaries.getValue(replenishingBackgroundLedger.id),
                        contentAlpha = 0f,
                        depthDimAlpha = LEDGER_STACK_BACKGROUND_DIM_ALPHA,
                    )
                }
                if (promotingBackgroundLedger != null) {
                    val startsFromFarLayer = ledgers.size > 2
                    val startScale = if (startsFromFarLayer) {
                        LEDGER_STACK_FAR_BACKGROUND_SCALE
                    } else {
                        LEDGER_STACK_PREWARM_BACKGROUND_SCALE
                    }
                    val startAlpha = if (startsFromFarLayer) {
                        LEDGER_STACK_FAR_BACKGROUND_ALPHA
                    } else {
                        0f
                    }
                    LedgerHeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .zIndex(0.5f)
                            .graphicsLayer {
                                scaleX = startScale +
                                    (LEDGER_STACK_NEAR_BACKGROUND_SCALE - startScale) * motionProgress
                                scaleY = startScale +
                                    (LEDGER_STACK_NEAR_BACKGROUND_SCALE - startScale) * motionProgress
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                translationY = backgroundOffsetPx * motionProgress + backgroundHandoffTranslation
                                alpha = startAlpha +
                                    (LEDGER_STACK_NEAR_BACKGROUND_ALPHA - startAlpha) * motionProgress
                            },
                        ledger = promotingBackgroundLedger,
                        summary = ledgerSummaries.getValue(promotingBackgroundLedger.id),
                        contentAlpha = 0f,
                        depthDimAlpha = LEDGER_STACK_BACKGROUND_DIM_ALPHA,
                    )
                }
                if (nearBackgroundLedger != null) {
                    LedgerHeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = LEDGER_STACK_BACKGROUND_OFFSET)
                            .zIndex(1f)
                            .graphicsLayer {
                                scaleX = LEDGER_STACK_NEAR_BACKGROUND_SCALE
                                scaleY = LEDGER_STACK_NEAR_BACKGROUND_SCALE
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                translationY = backgroundHandoffTranslation
                                alpha = LEDGER_STACK_NEAR_BACKGROUND_ALPHA * (1f - motionProgress)
                            },
                        ledger = nearBackgroundLedger,
                        summary = ledgerSummaries.getValue(nearBackgroundLedger.id),
                        contentAlpha = 0f,
                        depthDimAlpha = LEDGER_STACK_BACKGROUND_DIM_ALPHA,
                    )
                }
            }
            LedgerHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = if (multipleLedgers) LEDGER_STACK_FOREGROUND_TOP_INSET else 0.dp)
                    .zIndex(3f)
                    .graphicsLayer {
                        scaleX = 1f + (LEDGER_STACK_OUTGOING_SCALE - 1f) * motionProgress
                        scaleY = 1f + (LEDGER_STACK_OUTGOING_SCALE - 1f) * motionProgress
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        translationY = motionDirection * outgoingOffsetPx * motionProgress
                        alpha = 1f - motionProgress
                    },
                ledger = currentLedger,
                summary = ledgerSummaries.getValue(currentLedger.id),
            )
            if (incomingLedger != null) {
                LedgerHeroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = if (multipleLedgers) LEDGER_STACK_FOREGROUND_TOP_INSET else 0.dp)
                        .zIndex(2f)
                        .graphicsLayer {
                            scaleX = LEDGER_STACK_INCOMING_SCALE +
                                (1f - LEDGER_STACK_INCOMING_SCALE) * motionProgress
                            scaleY = LEDGER_STACK_INCOMING_SCALE +
                                (1f - LEDGER_STACK_INCOMING_SCALE) * motionProgress
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                            translationY = -motionDirection * incomingOffsetPx * (1f - motionProgress)
                            alpha = motionProgress
                        },
                    ledger = incomingLedger,
                    summary = ledgerSummaries.getValue(incomingLedger.id),
                )
            }
        }
    }
}

/** 绘制可随账本层级整体移动、缩放和淡化的完整 Hero Card。 */
@Composable
private fun LedgerHeroCard(
    modifier: Modifier = Modifier,
    ledger: LedgerRecord,
    summary: LedgerAssetSummary,
    contentAlpha: Float = 1f,
    depthDimAlpha: Float = 0f,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(0.dp),
    ) {
        LedgerHeroContent(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(LEDGER_HERO_ASPECT_RATIO)
                .squircleClip(16.dp),
            ledger = ledger,
            summary = summary,
            contentAlpha = contentAlpha,
            depthDimAlpha = depthDimAlpha,
        )
    }
}

/** 在完整账本 Hero Card 内绘制封面、汇总金额和层级遮罩。 */
@Composable
private fun LedgerHeroContent(
    modifier: Modifier,
    ledger: LedgerRecord,
    summary: LedgerAssetSummary,
    contentAlpha: Float = 1f,
    depthDimAlpha: Float = 0f,
) {
    Box(modifier = modifier) {
        LedgerCover(ledger.coverKey, Modifier.fillMaxSize())
        val heroTextColor = ledgerCoverTextColor(ledger.coverKey)
        Box(
            Modifier.fillMaxSize().background(
                if (heroTextColor == Color.White) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.22f),
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(HERO_CARD_CONTENT_PADDING)
                .graphicsLayer { alpha = contentAlpha },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "净资产",
                        color = heroTextColor.copy(alpha = 0.84f),
                        style = MiuixTheme.textStyles.body2,
                    )
                    Text(
                        text = ledger.name,
                        color = heroTextColor.copy(alpha = 0.84f),
                        style = MiuixTheme.textStyles.body2,
                    )
                }
                Text(
                    text = formatCurrencyAmount(summary.netAssets, summary.currencySymbol),
                    modifier = Modifier.padding(top = 5.dp),
                    color = heroTextColor,
                    fontWeight = FontWeight.Bold,
                    style = MiuixTheme.textStyles.title1,
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "总资产  ${formatCurrencyAmount(summary.totalAssets, summary.currencySymbol)}",
                    modifier = Modifier.weight(1f),
                    color = heroTextColor.copy(alpha = 0.92f),
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = "总负债  ${formatCurrencyAmount(summary.totalLiabilities, summary.currencySymbol)}",
                    color = heroTextColor.copy(alpha = 0.92f),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = depthDimAlpha }
                .background(Color.Black),
        )
    }
}

/**
 * 展示一种账户类型的小计和真实账户余额。
 */
@Composable
private fun HomeAccountGroup(
    modifier: Modifier = Modifier,
    type: AccountTypeEntity,
    accounts: List<AccountEntity>,
    balances: Map<AccountEntity, Long>,
    cnyBalances: Map<AccountEntity, Long>,
    currencies: Map<String, CurrencyEntity>,
    baseCurrency: CurrencyEntity,
    onOpenAccount: (Long) -> Unit,
) {
    var expanded by rememberSaveable(type.key) { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) -90f else 90f,
        animationSpec = folmeSpring(damping = 1f, response = 0.35f),
        label = "accountGroupArrow",
    )
    Card(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GROUPED_CARD_HEADER_MIN_HEIGHT)
                .clickable { expanded = !expanded }
                .padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = type.name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = formatDecimalAmount(accounts.sumOf { cnyBalances.getValue(it) }),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = if (expanded) "收起${type.name}" else "展开${type.name}",
                modifier = Modifier
                    .padding(start = 6.dp)
                    .rotate(arrowRotation)
                    .size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING),
                )
                accounts.forEach { account ->
                    HomeAccountRow(
                        account = account,
                        balance = balances.getValue(account),
                        currencySymbol = currencies.getValue(account.currencyKey).symbol,
                        convertedBalance = cnyBalances.getValue(account),
                        baseCurrency = baseCurrency,
                        onClick = { onOpenAccount(account.id) },
                    )
                }
            }
        }
    }
}

/**
 * 展示单个账户的类型图标、名称和实时余额。
 */
@Composable
private fun HomeAccountRow(
    account: AccountEntity,
    balance: Long,
    currencySymbol: String,
    convertedBalance: Long,
    baseCurrency: CurrencyEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = GROUPED_CARD_HORIZONTAL_PADDING,
                vertical = GROUPED_CARD_ROW_VERTICAL_PADDING,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(GROUPED_CARD_ICON_CONTAINER_SIZE)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.primary.copy(alpha = 0.10f),
                    cornerRadius = GROUPED_CARD_ICON_CORNER_SIZE,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AccountIcon(
                iconKey = account.iconKey,
                modifier = Modifier.size(GROUPED_CARD_ICON_SIZE),
            )
        }
        Text(
            text = account.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MiuixTheme.textStyles.body1,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrencyAmount(balance, currencySymbol),
                fontWeight = FontWeight.Medium,
                style = MiuixTheme.textStyles.body1,
            )
            if (account.currencyKey != baseCurrency.key) {
                Text(
                    text = "≈ ${formatCurrencyAmount(convertedBalance, baseCurrency.symbol)}",
                    modifier = Modifier.padding(top = 3.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
}

/**
 * 展示按时间倒序排列的全部账目。
 */
@Composable
fun DetailsScreen(
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
    onEditTransaction: (Long) -> Unit,
) {
    val ledger = uiState.ledgers.firstOrNull { it.id == uiState.currentLedgerId } ?: return
    val baseCurrency = uiState.currencies.firstOrNull { it.key == ledger.baseCurrencyKey }
    if (baseCurrency == null) {
        MainTabList(innerPadding = innerPadding) {
            item { EmptyCard(text = "币种数据异常，请恢复相关币种后重试") }
        }
        return
    }
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val exchangeSources = remember(uiState.transactions) {
        uiState.transactions
            .filter { it.transferDirection == TransferDirection.OUT && it.exchangeId != null }
            .associateBy { it.exchangeId!! }
    }
    val recordsByDate = remember(uiState.transactions) {
        uiState.transactions
            .filter { it.transferDirection != TransferDirection.OUT }
            .groupBy(::detailsRecordDate)
            .entries
            .sortedByDescending(Map.Entry<LocalDate, List<TransactionRecord>>::key)
    }
    val (todayExpense, monthExpense, monthIncome) = remember(recordsByDate, today, currentMonth) {
        var todayExpense = 0L
        var monthExpense = 0L
        var monthIncome = 0L
        recordsByDate.forEach { (date, records) ->
            if (date == today) {
                records.filter { it.type == TransactionType.EXPENSE }.forEach {
                    todayExpense += it.baseAmountMinor
                }
            }
            if (YearMonth.from(date) == currentMonth) {
                records.forEach { record ->
                    when (record.type) {
                        TransactionType.EXPENSE -> monthExpense += record.baseAmountMinor
                        TransactionType.INCOME -> monthIncome += record.baseAmountMinor
                        else -> Unit
                    }
                }
            }
        }
        Triple(todayExpense, monthExpense, monthIncome)
    }

    MainTabList(innerPadding = innerPadding) {
        item {
            DetailsSummaryCard(
                todayExpense = todayExpense,
                monthExpense = monthExpense,
                monthIncome = monthIncome,
                ledger = ledger,
                currencySymbol = baseCurrency.symbol,
            )
        }
        if (recordsByDate.isEmpty()) {
            item {
                EmptyCard(text = "暂无明细")
            }
        } else {
            recordsByDate.forEach { entry ->
                item(key = entry.key.toEpochDay()) {
                    DetailsDateGroup(
                        date = entry.key,
                        records = entry.value,
                        baseCurrencyKey = baseCurrency.key,
                        baseCurrencySymbol = baseCurrency.symbol,
                        exchangeSources = exchangeSources,
                        coloredTransactionAmountsEnabled = uiState.coloredTransactionAmountsEnabled,
                        onEditTransaction = onEditTransaction,
                    )
                }
            }
        }
    }
}

/**
 * 展示今日支出和本月收支摘要。
 */
@Composable
private fun DetailsSummaryCard(
    todayExpense: Long,
    monthExpense: Long,
    monthIncome: Long,
    ledger: com.vos.accounting.data.LedgerRecord,
    currencySymbol: String,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(LEDGER_HERO_ASPECT_RATIO)) {
        LedgerCover(ledger.coverKey, Modifier.fillMaxSize())
        val heroTextColor = ledgerCoverTextColor(ledger.coverKey)
        Box(Modifier.fillMaxSize().background(if (heroTextColor == Color.White) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.22f)))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "今日支出（元）",
                    color = heroTextColor.copy(alpha = 0.84f),
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = formatCurrencyAmount(todayExpense, currencySymbol),
                    modifier = Modifier.padding(top = 7.dp),
                    color = heroTextColor,
                    fontWeight = FontWeight.Bold,
                    style = MiuixTheme.textStyles.title2,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .squircleBackground(
                        color = MiuixTheme.colorScheme.surface.copy(alpha = 0.82f),
                        cornerRadius = 14.dp,
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DetailsSummaryMetric(
                    modifier = Modifier.weight(1f),
                    title = "本月支出（元）",
                    amount = monthExpense,
                    currencySymbol = currencySymbol,
                )
                DetailsSummaryMetric(
                    modifier = Modifier.weight(1f),
                    title = "本月收入（元）",
                    amount = monthIncome,
                    currencySymbol = currencySymbol,
                )
            }
        }
        }
    }
}

/**
 * 展示明细摘要中的一个月度金额。
 */
@Composable
private fun DetailsSummaryMetric(
    modifier: Modifier,
    title: String,
    amount: Long,
    currencySymbol: String,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
        Text(
            text = formatCurrencyAmount(amount, currencySymbol),
            modifier = Modifier.padding(top = 5.dp),
            color = MiuixTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

/**
 * 以一个 Card 展示同一自然日的收支小计和账目。
 */
@Composable
private fun DetailsDateGroup(
    date: LocalDate,
    records: List<TransactionRecord>,
    baseCurrencyKey: String,
    baseCurrencySymbol: String,
    exchangeSources: Map<Long, TransactionRecord>,
    coloredTransactionAmountsEnabled: Boolean,
    onEditTransaction: (Long) -> Unit,
) {
    val expense = records
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf(TransactionRecord::baseAmountMinor)
    val income = records
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::baseAmountMinor)
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GROUPED_CARD_HEADER_MIN_HEIGHT)
                .padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.format(DETAILS_DATE_FORMATTER),
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body2,
            )
            Text(
                text = "支${formatCurrencyAmount(expense, baseCurrencySymbol)} 收${formatCurrencyAmount(income, baseCurrencySymbol)}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING))
        records.forEach { record ->
            DetailsTransactionRow(
                record = record,
                baseCurrencyKey = baseCurrencyKey,
                baseCurrencySymbol = baseCurrencySymbol,
                exchangeSource = record.exchangeId?.let(exchangeSources::get),
                coloredTransactionAmountsEnabled = coloredTransactionAmountsEnabled,
                onClick = { onEditTransaction(record.id) },
            )
        }
    }
}

/**
 * 展示明细日期分组中的分类图标、描述、时间和金额。
 */
@Composable
private fun DetailsTransactionRow(
    record: TransactionRecord,
    baseCurrencyKey: String,
    baseCurrencySymbol: String,
    exchangeSource: TransactionRecord?,
    coloredTransactionAmountsEnabled: Boolean,
    onClick: () -> Unit,
) {
    val isTransfer = record.type == TransactionType.TRANSFER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isTransfer, onClick = onClick)
            .padding(
                horizontal = GROUPED_CARD_HORIZONTAL_PADDING,
                vertical = GROUPED_CARD_ROW_VERTICAL_PADDING,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(GROUPED_CARD_ICON_CONTAINER_SIZE)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.primary.copy(alpha = 0.10f),
                    cornerRadius = GROUPED_CARD_ICON_CORNER_SIZE,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isTransfer) {
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowUpDown,
                    contentDescription = null,
                    modifier = Modifier.size(GROUPED_CARD_ICON_SIZE),
                    tint = MiuixTheme.colorScheme.primary,
                )
            } else {
                CategoryIcon(
                    option = categoryIconOption(record.categoryIconKey, record.categoryName),
                    modifier = Modifier.size(GROUPED_CARD_ICON_SIZE),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = if (isTransfer) "币种兑换" else record.merchant.ifBlank { record.categoryName },
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = "${formatDetailsTime(record.occurredAt)} | ${detailsRecordDescription(record)}",
                modifier = Modifier.padding(top = 3.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        val sign = if (record.type == TransactionType.EXPENSE) "-" else "+"
        Column(
            modifier = Modifier.padding(start = 12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = if (exchangeSource == null) {
                    "$sign${formatCurrencyAmount(record.amountMinor, record.currencySymbol)}"
                } else {
                    formatCurrencyAmount(exchangeSource.amountMinor, exchangeSource.currencySymbol)
                },
                fontWeight = FontWeight.Bold,
                color = transactionAmountColor(
                    type = record.type,
                    enabled = coloredTransactionAmountsEnabled,
                    defaultColor = MiuixTheme.colorScheme.onSurface,
                ),
                style = MiuixTheme.textStyles.body1,
            )
            if (exchangeSource != null || record.currencyKey != baseCurrencyKey) {
                Text(
                    text = if (exchangeSource == null) {
                        "≈ $sign${formatCurrencyAmount(record.baseAmountMinor, baseCurrencySymbol)}"
                    } else {
                        "→ ${formatCurrencyAmount(record.amountMinor, record.currencySymbol)}"
                    },
                    modifier = Modifier.padding(top = 3.dp),
                    color = transactionAmountColor(
                        type = record.type,
                        enabled = coloredTransactionAmountsEnabled,
                        defaultColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    ),
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
}

/**
 * 返回账目在设备时区对应的自然日。
 */
private fun detailsRecordDate(record: TransactionRecord): LocalDate =
    transactionLocalDate(record.occurredAt)

/**
 * 返回明细账目副标题中的备注或账户说明。
 */
private fun detailsRecordDescription(record: TransactionRecord): String = record.note
    .ifBlank { record.accountName }

/**
 * 格式化明细账目发生时间。
 */
private fun formatDetailsTime(timestamp: Long): String = Instant
    .ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DETAILS_TIME_FORMATTER)

private val DETAILS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd EEEE", Locale.CHINA)
private val DETAILS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

/**
 * 展示收支总览与支出分类排行。
 */
@Composable
fun StatisticsScreen(
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
) {
    StatisticsReportContent(
        uiState = uiState,
        innerPadding = innerPadding,
    )
}

/**
 * 展示外观与跟随系统配色设置。
 */
@Composable
fun SettingsScreen(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onThemeModeChange: (AccountingThemeMode) -> Unit,
    onFollowSystemColorChange: (Boolean) -> Unit,
    onPredictiveBackAnimationEnabledChange: (Boolean) -> Unit,
    onColoredTransactionAmountsEnabledChange: (Boolean) -> Unit,
) {
    var showThemePopup by rememberSaveable { mutableStateOf(false) }
    SecondaryScaffold(
        title = "设置",
        backdrop = backdrop,
        onBack = onBack,
    ) { innerPadding ->
        SecondaryList(innerPadding = innerPadding) {
            item {
                SectionTitle(text = "通用")
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        BasicComponent(
                            title = "外观",
                            modifier = Modifier.fillMaxWidth(),
                            endActions = {
                                Text(
                                    text = accountingThemeModeTitle(uiState.themeMode),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    style = MiuixTheme.textStyles.body2,
                                )
                                Icon(
                                    imageVector = MiuixIcons.Basic.ArrowUpDown,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(width = 10.dp, height = 16.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            },
                            onClick = { showThemePopup = true },
                        )
                        AccountingThemeModePopup(
                            show = showThemePopup,
                            selectedMode = uiState.themeMode,
                            onDismiss = { showThemePopup = false },
                            onSelect = {
                                onThemeModeChange(it)
                                showThemePopup = false
                            },
                        )
                    }
                    SwitchPreference(
                        checked = uiState.followSystemColor,
                        onCheckedChange = onFollowSystemColorChange,
                        title = "跟随系统配色",
                        summary = "关闭后使用固定品牌配色",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.predictiveBackAnimationEnabled,
                        onCheckedChange = onPredictiveBackAnimationEnabledChange,
                        title = "预测性返回动画",
                        summary = "开启后边缘返回会随手势移动",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchPreference(
                        checked = uiState.coloredTransactionAmountsEnabled,
                        onCheckedChange = onColoredTransactionAmountsEnabledChange,
                        title = "收支金额颜色",
                        summary = "支出红色收入绿色",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                SectionTitle(text = "数据")
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    BasicComponent(
                        title = "数据备份与恢复",
                        summary = "密码导出全部数据，或从备份覆盖恢复",
                        modifier = Modifier.fillMaxWidth(),
                        endActions = {
                            Icon(
                                imageVector = MiuixIcons.Basic.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(width = 10.dp, height = 16.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = onOpenBackup,
                    )
                }
            }
        }
    }
}

/**
 * 返回外观模式的展示文案。
 */
private fun accountingThemeModeTitle(mode: AccountingThemeMode): String = when (mode) {
    AccountingThemeMode.SYSTEM -> "跟随系统"
    AccountingThemeMode.LIGHT -> "浅色"
    AccountingThemeMode.DARK -> "深色"
}

/**
 * 以 HyperOS 风格弹出列表展示可选外观模式。
 */
@Composable
private fun AccountingThemeModePopup(
    show: Boolean,
    selectedMode: AccountingThemeMode,
    onDismiss: () -> Unit,
    onSelect: (AccountingThemeMode) -> Unit,
) {
    WindowListPopup(
        show = show,
        popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(
            horizontalMargin = 12.dp,
        ),
        alignment = PopupPositionProvider.Align.End,
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        maxHeight = 440.dp,
        minWidth = 288.dp,
    ) {
        ListPopupColumn {
            AccountingThemeMode.entries.forEach { mode ->
                PopupSelectionRow(
                    title = accountingThemeModeTitle(mode),
                    summary = when (mode) {
                        AccountingThemeMode.SYSTEM -> "跟随系统明暗设置"
                        AccountingThemeMode.LIGHT -> "始终使用浅色外观"
                        AccountingThemeMode.DARK -> "始终使用深色外观"
                    },
                    selected = mode == selectedMode,
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

/**
 * 建立遵守统一首尾间距、横向安全区和 800dp 上限的主标签列表。
 */
@Composable
internal fun MainTabList(
    innerPadding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
            item {
                Spacer(
                    modifier = Modifier.height(
                        24.dp + innerPadding.calculateBottomPadding(),
                    ),
                )
            }
        }
    }
}

/**
 * 展示主列表中的分区标题。
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 5.dp, bottom = 9.dp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontWeight = FontWeight.Medium,
        style = MiuixTheme.textStyles.body2,
    )
}

/**
 * 展示无数据时的静态说明卡片。
 */
@Composable
internal fun EmptyCard(text: String) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
    }
}
