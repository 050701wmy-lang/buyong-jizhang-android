package com.vos.accounting.ui

import androidx.compose.animation.AnimatedVisibility
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
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    val totalAssets = accountBalances.values.sumOf { maxOf(it, 0L) }
    val totalLiabilities = accountBalances.values.sumOf { -minOf(it, 0L) }

    MainTabList(innerPadding = innerPadding) {
        item {
            HomeAssetCard(
                netAssets = totalAssets - totalLiabilities,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
            )
        }
        AccountType.entries.forEach { type ->
            val accounts = activeAccounts.filter { it.type == type }
            if (accounts.isNotEmpty()) {
                item(key = type.name) {
                    HomeAccountGroup(
                        type = type,
                        accounts = accounts,
                        balances = accountBalances,
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
                .height(168.dp)
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
                    .padding(20.dp),
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
    type: AccountType,
    accounts: List<AccountEntity>,
    balances: Map<AccountEntity, Long>,
    onOpenAccount: (Long) -> Unit,
) {
    var expanded by rememberSaveable(type.name) { mutableStateOf(true) }
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
                .clickable { expanded = !expanded }
                .padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = accountTypeTitle(type),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = formatDecimalAmount(accounts.sumOf { balances.getValue(it) }),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = if (expanded) "收起${accountTypeTitle(type)}" else "展开${accountTypeTitle(type)}",
                modifier = Modifier
                    .padding(start = 6.dp)
                    .rotate(if (expanded) -90f else 90f)
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
                imageVector = accountTypeIcon(account.type),
                contentDescription = null,
                modifier = Modifier.size(GROUPED_CARD_ICON_SIZE),
                tint = MiuixTheme.colorScheme.primary.copy(alpha = 0.76f),
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
            text = formatDecimalAmount(balance),
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
        .sumOf(TransactionRecord::amountMinor)
    val monthRecords = uiState.transactions.filter {
        YearMonth.from(detailsRecordDate(it)) == currentMonth
    }

    MainTabList(innerPadding = innerPadding) {
        item {
            DetailsSummaryCard(
                todayExpense = todayExpense,
                monthExpense = monthRecords
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf(TransactionRecord::amountMinor),
                monthIncome = monthRecords
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf(TransactionRecord::amountMinor),
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今日支出（元）",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            Text(
                text = formatDecimalAmount(todayExpense),
                modifier = Modifier.padding(top = 7.dp, bottom = 16.dp),
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.title2,
            )
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
        .sumOf(TransactionRecord::amountMinor)
    val income = records
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::amountMinor)
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
                "-${formatDecimalAmount(record.amountMinor)}"
            } else {
                "+${formatDecimalAmount(record.amountMinor)}"
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
 * 展示当前版本的外观、数据与智能记账配置摘要。
 */
@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    MainTabList(innerPadding = innerPadding) {
        item {
            SectionTitle(text = "通用")
        }
        item {
            SettingsGroup(
                rows = listOf(
                    "货币" to "人民币（CNY）",
                    "外观" to "跟随系统",
                    "数据存储" to "仅保存在本机",
                ),
            )
        }
        item {
            SectionTitle(text = "数据保护")
        }
        item {
            SettingsGroup(
                rows = listOf(
                    "系统备份" to "已关闭",
                    "卸载应用" to "将删除本地账本",
                    "应用内备份" to "后续版本提供",
                ),
            )
        }
        item {
            SectionTitle(text = "智能记账")
        }
        item {
            SettingsGroup(
                rows = listOf(
                    "解析方式" to "本地自然语言",
                    "写入规则" to "确认草稿后入账",
                ),
            )
        }
        item {
            Text(
                text = "随记 0.1.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                style = MiuixTheme.textStyles.footnote1,
            )
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
private fun EmptyCard(text: String) {
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

/**
 * 以一个连续 Card 展示设置名称和值。
 */
@Composable
private fun SettingsGroup(rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = row.first)
                Text(
                    text = row.second,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}
