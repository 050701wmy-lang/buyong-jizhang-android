package com.vos.accounting.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.data.amountInCnyMinor
import com.vos.accounting.data.convertToCnyMinor
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.window.WindowListPopup
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

/** 顶部数据 Hero Card 的统一高度。 */
internal val HERO_CARD_HEIGHT = 168.dp

/** 顶部数据 Hero Card 的统一内容内边距。 */
internal val HERO_CARD_CONTENT_PADDING = 20.dp

/**
 * 展示余额、快捷记账入口与最近账目。
 */
@Composable
fun HomeScreen(
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
    onOpenAccount: (Long) -> Unit,
) {
    val activeAccounts = uiState.accounts.filterNot(AccountEntity::isArchived)
    val accountBalances = activeAccounts.associateWith { account ->
        calculateAccountBalance(account, uiState.transactions)
    }
    val currencies = uiState.currencies.associateBy(CurrencyEntity::key)
    val cnyBalances = accountBalances.mapValues { (account, balance) ->
        convertToCnyMinor(balance, currencies.getValue(account.currencyKey).rateToCnyScaled)
    }
    val totalAssets = cnyBalances.values.sumOf { maxOf(it, 0L) }
    val totalLiabilities = cnyBalances.values.sumOf { -minOf(it, 0L) }

    MainTabList(innerPadding = innerPadding) {
        item {
            HomeAssetCard(
                netAssets = totalAssets - totalLiabilities,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
            )
        }
        uiState.accountTypes.forEach { type ->
            val accounts = activeAccounts.filter { it.typeKey == type.key }
            if (accounts.isNotEmpty()) {
                item(key = type.key) {
                    HomeAccountGroup(
                        modifier = Modifier.animateItem(),
                        type = type,
                        accounts = accounts,
                        balances = accountBalances,
                        cnyBalances = cnyBalances,
                        currencies = currencies,
                        onOpenAccount = onOpenAccount,
                    )
                }
            }
        }
    }
}

/**
 * 展示净资产、总资产和总负债。
 */
@Composable
private fun HomeAssetCard(
    netAssets: Long,
    totalAssets: Long,
    totalLiabilities: Long,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_CARD_HEIGHT)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MiuixTheme.colorScheme.primary,
                            MiuixTheme.colorScheme.primary.copy(alpha = 0.58f),
                            MiuixTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 24.dp)
                    .size(92.dp)
                    .squircleBackground(
                        color = Color.White.copy(alpha = 0.08f),
                        cornerRadius = 30.dp,
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(HERO_CARD_CONTENT_PADDING),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "净资产",
                        color = Color.White.copy(alpha = 0.84f),
                        style = MiuixTheme.textStyles.body2,
                    )
                    Text(
                        text = formatDecimalAmount(netAssets),
                        modifier = Modifier.padding(top = 5.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MiuixTheme.textStyles.title1,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "总资产  ${formatDecimalAmount(totalAssets)}",
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.92f),
                        style = MiuixTheme.textStyles.body2,
                    )
                    Text(
                        text = "总负债  ${formatDecimalAmount(totalLiabilities)}",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
        }
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
        Text(
            text = formatCurrencyAmount(balance, currencySymbol),
            fontWeight = FontWeight.Medium,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

/**
 * 根据账户期初余额和全部收支计算实时余额。
 */
private fun calculateAccountBalance(
    account: AccountEntity,
    records: List<TransactionRecord>,
): Long = account.openingBalanceMinor + records
    .filter { it.accountId == account.id }
    .sumOf { record ->
        if (record.type == TransactionType.INCOME) record.amountMinor else -record.amountMinor
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
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val recordsByDate = uiState.transactions
        .groupBy(::detailsRecordDate)
        .entries
        .sortedByDescending(Map.Entry<LocalDate, List<TransactionRecord>>::key)
    val todayExpense = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE && detailsRecordDate(it) == today }
        .sumOf(TransactionRecord::amountInCnyMinor)
    val monthRecords = uiState.transactions.filter {
        YearMonth.from(detailsRecordDate(it)) == currentMonth
    }

    MainTabList(innerPadding = innerPadding) {
        item {
            DetailsSummaryCard(
                todayExpense = todayExpense,
                monthExpense = monthRecords
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf(TransactionRecord::amountInCnyMinor),
                monthIncome = monthRecords
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf(TransactionRecord::amountInCnyMinor),
            )
        }
        if (uiState.transactions.isEmpty()) {
            item {
                EmptyCard(text = "暂无明细")
            }
        } else {
            recordsByDate.forEach { entry ->
                item(key = entry.key.toEpochDay()) {
                    DetailsDateGroup(
                        date = entry.key,
                        records = entry.value,
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
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.primary.copy(alpha = 0.09f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_CARD_HEIGHT)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "今日支出（元）",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = formatDecimalAmount(todayExpense),
                    modifier = Modifier.padding(top = 7.dp),
                    color = MiuixTheme.colorScheme.primary,
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
                )
                DetailsSummaryMetric(
                    modifier = Modifier.weight(1f),
                    title = "本月收入（元）",
                    amount = monthIncome,
                )
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
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
        Text(
            text = formatDecimalAmount(amount),
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
    onEditTransaction: (Long) -> Unit,
) {
    val expense = records
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf(TransactionRecord::amountInCnyMinor)
    val income = records
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::amountInCnyMinor)
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
                text = "支${formatDecimalAmount(expense)} 收${formatDecimalAmount(income)}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING))
        records.forEach { record ->
            DetailsTransactionRow(
                record = record,
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
            Icon(
                imageVector = accountingCategoryIcon(
                    iconKey = record.categoryIconKey,
                    fallbackName = record.categoryName,
                ),
                contentDescription = null,
                modifier = Modifier.size(GROUPED_CARD_ICON_SIZE),
                tint = MiuixTheme.colorScheme.primary.copy(alpha = 0.72f),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = record.merchant.ifBlank { record.categoryName },
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
        Text(
            text = if (record.type == TransactionType.EXPENSE) {
                "-${formatCurrencyAmount(record.amountMinor, record.currencySymbol)}"
            } else {
                "+${formatCurrencyAmount(record.amountMinor, record.currencySymbol)}"
            },
            modifier = Modifier.padding(start = 12.dp),
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.body1,
        )
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
    onThemeModeChange: (AccountingThemeMode) -> Unit,
    onFollowSystemColorChange: (Boolean) -> Unit,
    onPredictiveBackAnimationEnabledChange: (Boolean) -> Unit,
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
 * 展示首页收入或支出的小型汇总项。
 */
@Composable
private fun SummaryAmount(
    modifier: Modifier,
    label: String,
    amount: Long,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
        Text(
            text = formatMoney(amount),
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = FontWeight.Medium,
            style = MiuixTheme.textStyles.body1,
        )
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
 * 以一个连续 Card 展示多笔账目。
 */
@Composable
private fun TransactionGroup(records: List<TransactionRecord>) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        records.forEachIndexed { index, record ->
            TransactionRow(record = record)
            if (index < records.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

/**
 * 展示一笔账目的描述、分类、时间和金额。
 */
@Composable
private fun TransactionRow(record: TransactionRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.merchant.ifBlank { record.categoryName },
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                if (record.source == TransactionSource.AI) {
                    Text(
                        text = "AI",
                        modifier = Modifier.padding(start = 7.dp),
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                }
            }
            Text(
                text = "${record.categoryName} · ${record.accountName} · ${formatTransactionTime(record.occurredAt)}",
                modifier = Modifier.padding(top = 4.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        Text(
            text = if (record.type == TransactionType.EXPENSE) {
                "-${formatMoney(record.amountMinor)}"
            } else {
                "+${formatMoney(record.amountMinor)}"
            },
            modifier = Modifier.padding(start = 12.dp),
            color = if (record.type == TransactionType.EXPENSE) {
                MiuixTheme.colorScheme.error
            } else {
                MiuixTheme.colorScheme.primary
            },
            fontWeight = FontWeight.Medium,
        )
    }
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
