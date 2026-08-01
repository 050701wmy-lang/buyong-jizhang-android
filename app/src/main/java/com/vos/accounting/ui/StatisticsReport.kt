package com.vos.accounting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

/**
 * 表示统计报表的聚合周期。
 */
private enum class ReportPeriod(
    val tabTitle: String,
    val currentTitle: String,
    val previousTitle: String,
) {
    WEEK("周报", "本周", "上周"),
    MONTH("月报", "本月", "上月"),
    YEAR("年报", "本年", "上年"),
}

/**
 * 表示一个首尾日期均包含在内的报表区间。
 */
private data class ReportDateRange(
    val start: LocalDate,
    val end: LocalDate,
)

/**
 * 表示趋势图中的一个聚合点。
 */
private data class ReportPoint(
    val label: String,
    val amountMinor: Long,
)

/**
 * 表示分类构成中的一个聚合项。
 */
private data class ReportCategory(
    val name: String,
    val amountMinor: Long,
)

/**
 * 汇总统计页单次展示所需的所有派生数据。
 */
private data class ReportData(
    val range: ReportDateRange,
    val currentTotalMinor: Long,
    val dailyAverageMinor: Long,
    val comparedWithPreviousMinor: Long,
    val balanceMinor: Long,
    val currentTrend: List<ReportPoint>,
    val recentTrend: List<ReportPoint>,
    val categories: List<ReportCategory>,
)

/**
 * 根据参考图组织周期控制、摘要、趋势和分类构成。
 */
@Composable
internal fun StatisticsReportContent(
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
) {
    val today = LocalDate.now()
    var period by rememberSaveable { mutableStateOf(ReportPeriod.WEEK) }
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var anchorEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val anchor = LocalDate.ofEpochDay(anchorEpochDay)
    val reportData = remember(uiState.transactions, period, selectedType, anchorEpochDay) {
        buildReportData(
            records = uiState.transactions,
            period = period,
            selectedType = selectedType,
            anchor = anchor,
        )
    }

    MainTabList(innerPadding = innerPadding) {
        item {
            ReportPeriodTabs(
                period = period,
                onSelect = {
                    period = it
                    anchorEpochDay = today.toEpochDay()
                },
            )
        }
        item {
            ReportRangeControls(
                period = period,
                selectedType = selectedType,
                range = reportData.range,
                canMoveForward = reportData.range.end.isBefore(today),
                onMoveBackward = {
                    anchorEpochDay = shiftAnchor(anchor, period, -1).toEpochDay()
                },
                onMoveForward = {
                    anchorEpochDay = shiftAnchor(anchor, period, 1).toEpochDay()
                },
                onSelectType = { selectedType = it },
            )
        }
        item {
            ReportSummaryCard(
                period = period,
                selectedType = selectedType,
                data = reportData,
            )
        }
        item {
            ReportChartCard(
                title = "${period.currentTitle}趋势",
                points = reportData.currentTrend,
            )
        }
        item {
            ReportChartCard(
                title = "${typeTitle(selectedType)}趋势",
                points = reportData.recentTrend,
            )
        }
        item {
            ReportCategoryCard(
                title = "${typeTitle(selectedType)}分类构成",
                categories = reportData.categories,
            )
        }
    }
}

/**
 * 展示周报、月报与年报的 MIUIX 标签切换。
 */
@Composable
private fun ReportPeriodTabs(
    period: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReportPeriod.entries.forEachIndexed { index, item ->
                ReportPeriodTab(
                    modifier = Modifier.weight(1f),
                    period = item,
                    selected = period == item,
                    onClick = { onSelect(item) },
                )
                if (index < ReportPeriod.entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(
                                MiuixTheme.colorScheme.outline.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * 展示整体分段栏中的单个周期入口。
 */
@Composable
private fun ReportPeriodTab(
    modifier: Modifier,
    period: ReportPeriod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(3.dp)
            .squircleBackground(
                color = if (selected) {
                    MiuixTheme.colorScheme.surface
                } else {
                    MiuixTheme.colorScheme.surface.copy(alpha = 0f)
                },
                cornerRadius = 14.dp,
            )
            .squircleClip(14.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = reportPeriodIcon(period),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
        )
        Text(
            text = period.tabTitle,
            modifier = Modifier.padding(start = 7.dp),
            color = if (selected) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

/**
 * 返回各报表周期对应的 MIUIX 图标。
 */
private fun reportPeriodIcon(period: ReportPeriod): ImageVector = when (period) {
    ReportPeriod.WEEK -> MiuixIcons.Weeks
    ReportPeriod.MONTH -> MiuixIcons.Months
    ReportPeriod.YEAR -> MiuixIcons.GridView
}

/**
 * 展示日期区间移动和支出收入切换。
 */
@Composable
private fun ReportRangeControls(
    period: ReportPeriod,
    selectedType: TransactionType,
    range: ReportDateRange,
    canMoveForward: Boolean,
    onMoveBackward: () -> Unit,
    onMoveForward: () -> Unit,
    onSelectType: (TransactionType) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onMoveBackward,
                    backgroundColor = MiuixTheme.colorScheme.surface,
                    minWidth = 35.dp,
                    minHeight = 35.dp,
                ) {
                    Icon(
                        imageVector = MiuixIcons.ChevronBackward,
                        contentDescription = "上一周期",
                    )
                }
                Text(
                    text = formatRange(period, range),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    style = MiuixTheme.textStyles.body2,
                )
                IconButton(
                    onClick = onMoveForward,
                    enabled = canMoveForward,
                    backgroundColor = MiuixTheme.colorScheme.surface,
                    minWidth = 35.dp,
                    minHeight = 35.dp,
                ) {
                    Icon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = "下一周期",
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            ReportTypeSwitch(
                modifier = Modifier.width(118.dp),
                selectedType = selectedType,
                onSelectType = onSelectType,
            )
        }
    }
}

/**
 * 展示无独立描边的支出与收入切换器。
 */
@Composable
private fun ReportTypeSwitch(
    modifier: Modifier,
    selectedType: TransactionType,
    onSelectType: (TransactionType) -> Unit,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .squircleBackground(
                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.10f),
                cornerRadius = 14.dp,
            ),
    ) {
        ReportTypeOption(
            modifier = Modifier.weight(1f),
            title = "支出",
            selected = selectedType == TransactionType.EXPENSE,
            onClick = { onSelectType(TransactionType.EXPENSE) },
        )
        ReportTypeOption(
            modifier = Modifier.weight(1f),
            title = "收入",
            selected = selectedType == TransactionType.INCOME,
            onClick = { onSelectType(TransactionType.INCOME) },
        )
    }
}

/**
 * 展示收支切换器中的单个选项。
 */
@Composable
private fun ReportTypeOption(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(3.dp)
            .squircleBackground(
                color = if (selected) {
                    MiuixTheme.colorScheme.surface
                } else {
                    MiuixTheme.colorScheme.surface.copy(alpha = 0f)
                },
                cornerRadius = 12.dp,
            )
            .squircleClip(12.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = if (selected) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

/**
 * 展示本期总额、日均、环比变化和收支结余。
 */
@Composable
private fun ReportSummaryCard(
    period: ReportPeriod,
    selectedType: TransactionType,
    data: ReportData,
) {
    val typeTitle = typeTitle(selectedType)
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ReportMetric(
                modifier = Modifier.weight(1f),
                label = "${period.currentTitle}$typeTitle（元）",
                valueMinor = data.currentTotalMinor,
            )
            ReportMetric(
                modifier = Modifier.weight(1f),
                label = "日均$typeTitle（元）",
                valueMinor = data.dailyAverageMinor,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ReportMetric(
                modifier = Modifier.weight(1f),
                label = "比${period.previousTitle}$typeTitle（元）",
                valueMinor = data.comparedWithPreviousMinor,
                showSign = true,
            )
            ReportMetric(
                modifier = Modifier.weight(1f),
                label = "收支结余（元）",
                valueMinor = data.balanceMinor,
                showSign = true,
            )
        }
    }
}

/**
 * 展示摘要 Card 中的一项名称和值。
 */
@Composable
private fun ReportMetric(
    modifier: Modifier,
    label: String,
    valueMinor: Long,
    showSign: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(13.dp)
                    .squircleBackground(
                        color = MiuixTheme.colorScheme.primary,
                        cornerRadius = 3.dp,
                    ),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 7.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        Text(
            text = formatReportAmount(valueMinor, showSign),
            modifier = Modifier.padding(top = 5.dp),
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.title3,
        )
    }
}

/**
 * 展示一个当前周期或历史周期趋势 Card。
 */
@Composable
private fun ReportChartCard(
    title: String,
    points: List<ReportPoint>,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.title4,
        )
        if (points.none { it.amountMinor > 0 }) {
            EmptyReportContent()
        } else {
            ReportBarChart(points = points)
        }
    }
}

/**
 * 使用真实聚合金额绘制 MIUIX squircle 柱状趋势图。
 */
@Composable
private fun ReportBarChart(points: List<ReportPoint>) {
    val maxAmount = points.maxOf(ReportPoint::amountMinor)
    val chartWidth = max(336, points.size * 48).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .width(chartWidth)
                .height(210.dp)
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { point ->
                val fraction = point.amountMinor.toFloat() / maxAmount.toFloat()
                val barHeight = max(6f, 132f * fraction).dp
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = compactAmount(point.amountMinor),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .width(28.dp)
                            .height(barHeight)
                            .squircleBackground(
                                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.32f),
                                cornerRadius = 8.dp,
                            ),
                    )
                    Text(
                        text = point.label,
                        modifier = Modifier.padding(top = 7.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                }
            }
        }
    }
}

/**
 * 展示当前周期按分类聚合的金额、占比和进度。
 */
@Composable
private fun ReportCategoryCard(
    title: String,
    categories: List<ReportCategory>,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.title4,
        )
        if (categories.isEmpty()) {
            EmptyReportContent()
        } else {
            val total = categories.sumOf(ReportCategory::amountMinor)
            categories.forEach { category ->
                val fraction = category.amountMinor.toFloat() / total.toFloat()
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = category.name)
                        Text(
                            text = "${formatReportAmount(category.amountMinor)} · ${formatPercentage(fraction)}",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(8.dp)
                            .squircleBackground(
                                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                                cornerRadius = 4.dp,
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .squircleBackground(
                                    color = MiuixTheme.colorScheme.primary,
                                    cornerRadius = 4.dp,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 展示统计 Card 的无数据状态。
 */
@Composable
private fun EmptyReportContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .background(
                    color = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Send,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MiuixTheme.colorScheme.primary.copy(alpha = 0.52f),
            )
        }
        Text(
            text = "暂无记录~",
            modifier = Modifier.padding(top = 12.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

/**
 * 根据完整账目列表计算当前报表数据。
 */
private fun buildReportData(
    records: List<TransactionRecord>,
    period: ReportPeriod,
    selectedType: TransactionType,
    anchor: LocalDate,
): ReportData {
    val range = rangeFor(period, anchor)
    val previousRange = rangeFor(period, shiftAnchor(anchor, period, -1))
    val currentRecords = records.filter { recordDate(it) in range.start..range.end }
    val currentTypeRecords = currentRecords.filter { it.type == selectedType }
    val currentTotal = currentTypeRecords.sumOf(TransactionRecord::amountMinor)
    val previousTotal = records
        .filter { it.type == selectedType && recordDate(it) in previousRange.start..previousRange.end }
        .sumOf(TransactionRecord::amountMinor)
    val dayCount = ChronoUnit.DAYS.between(range.start, range.end) + 1
    val income = currentRecords
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::amountMinor)
    val expense = currentRecords
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf(TransactionRecord::amountMinor)

    return ReportData(
        range = range,
        currentTotalMinor = currentTotal,
        dailyAverageMinor = currentTotal / dayCount,
        comparedWithPreviousMinor = currentTotal - previousTotal,
        balanceMinor = income - expense,
        currentTrend = currentTrendPoints(records, period, selectedType, range),
        recentTrend = recentTrendPoints(records, period, selectedType, anchor),
        categories = currentTypeRecords
            .groupBy(TransactionRecord::categoryName)
            .map { (name, groupedRecords) ->
                ReportCategory(
                    name = name,
                    amountMinor = groupedRecords.sumOf(TransactionRecord::amountMinor),
                )
            }
            .sortedByDescending(ReportCategory::amountMinor),
    )
}

/**
 * 按周、月或年计算包含指定锚点的日期区间。
 */
private fun rangeFor(
    period: ReportPeriod,
    anchor: LocalDate,
): ReportDateRange = when (period) {
    ReportPeriod.WEEK -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        ReportDateRange(start, start.plusDays(6))
    }

    ReportPeriod.MONTH -> {
        val month = YearMonth.from(anchor)
        ReportDateRange(month.atDay(1), month.atEndOfMonth())
    }

    ReportPeriod.YEAR -> ReportDateRange(
        LocalDate.of(anchor.year, 1, 1),
        LocalDate.of(anchor.year, 12, 31),
    )
}

/**
 * 将报表锚点移动指定数量的周期。
 */
private fun shiftAnchor(
    anchor: LocalDate,
    period: ReportPeriod,
    amount: Long,
): LocalDate = when (period) {
    ReportPeriod.WEEK -> anchor.plusWeeks(amount)
    ReportPeriod.MONTH -> anchor.plusMonths(amount)
    ReportPeriod.YEAR -> anchor.plusYears(amount)
}

/**
 * 按周期内部的日、周或月生成趋势点。
 */
private fun currentTrendPoints(
    records: List<TransactionRecord>,
    period: ReportPeriod,
    selectedType: TransactionType,
    range: ReportDateRange,
): List<ReportPoint> = when (period) {
    ReportPeriod.WEEK -> (0L..6L).map { offset ->
        val date = range.start.plusDays(offset)
        ReportPoint(
            label = WEEKDAY_LABELS[offset.toInt()],
            amountMinor = amountForRange(records, selectedType, date, date),
        )
    }

    ReportPeriod.MONTH -> {
        val weekCount = (range.end.dayOfMonth + 6) / 7
        (0 until weekCount).map { index ->
            val start = range.start.plusDays((index * 7).toLong())
            val end = minOf(start.plusDays(6), range.end)
            ReportPoint(
                label = "${index + 1}周",
                amountMinor = amountForRange(records, selectedType, start, end),
            )
        }
    }

    ReportPeriod.YEAR -> (1..12).map { month ->
        val monthRange = YearMonth.of(range.start.year, month)
        ReportPoint(
            label = "${month}月",
            amountMinor = amountForRange(
                records,
                selectedType,
                monthRange.atDay(1),
                monthRange.atEndOfMonth(),
            ),
        )
    }
}

/**
 * 生成包含当前周期在内的最近六个周期趋势点。
 */
private fun recentTrendPoints(
    records: List<TransactionRecord>,
    period: ReportPeriod,
    selectedType: TransactionType,
    anchor: LocalDate,
): List<ReportPoint> = (5 downTo 0).map { offset ->
    val pointAnchor = shiftAnchor(anchor, period, -offset.toLong())
    val pointRange = rangeFor(period, pointAnchor)
    ReportPoint(
        label = recentPointLabel(period, pointRange, offset == 0),
        amountMinor = amountForRange(
            records,
            selectedType,
            pointRange.start,
            pointRange.end,
        ),
    )
}

/**
 * 汇总指定日期区间和收支类型的金额。
 */
private fun amountForRange(
    records: List<TransactionRecord>,
    selectedType: TransactionType,
    start: LocalDate,
    end: LocalDate,
): Long = records
    .filter { it.type == selectedType && recordDate(it) in start..end }
    .sumOf(TransactionRecord::amountMinor)

/**
 * 将账目时间戳转换为设备时区日期。
 */
private fun recordDate(record: TransactionRecord): LocalDate = Instant
    .ofEpochMilli(record.occurredAt)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

/**
 * 格式化顶部日期区间。
 */
private fun formatRange(
    period: ReportPeriod,
    range: ReportDateRange,
): String = when (period) {
    ReportPeriod.WEEK -> {
        val start = range.start.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        val end = range.end.format(DateTimeFormatter.ofPattern("MM.dd"))
        "$start~$end"
    }

    ReportPeriod.MONTH -> range.start.format(DateTimeFormatter.ofPattern("yyyy.MM"))
    ReportPeriod.YEAR -> range.start.year.toString()
}

/**
 * 返回历史趋势点的短标签。
 */
private fun recentPointLabel(
    period: ReportPeriod,
    range: ReportDateRange,
    isCurrent: Boolean,
): String {
    if (isCurrent) return period.currentTitle
    return when (period) {
        ReportPeriod.WEEK -> "${range.start.get(WeekFields.ISO.weekOfWeekBasedYear())}周"
        ReportPeriod.MONTH -> "${range.start.monthValue}月"
        ReportPeriod.YEAR -> range.start.year.toString()
    }
}

/**
 * 返回当前收支类型的中文名称。
 */
private fun typeTitle(type: TransactionType): String =
    if (type == TransactionType.EXPENSE) "支出" else "收入"

/**
 * 将最小货币单位格式化为报表中的两位小数。
 */
private fun formatReportAmount(
    amountMinor: Long,
    showSign: Boolean = false,
): String {
    val amount = BigDecimal.valueOf(amountMinor, 2)
    val prefix = if (showSign && amountMinor > 0) "+" else ""
    return "$prefix${amount.setScale(2).toPlainString()}"
}

/**
 * 将图表金额格式化为紧凑文本。
 */
private fun compactAmount(amountMinor: Long): String {
    val amount = BigDecimal.valueOf(amountMinor, 2)
    return if (amount >= BigDecimal("10000")) {
        "${amount.movePointLeft(4).setScale(1, java.math.RoundingMode.HALF_UP)}万"
    } else {
        amount.stripTrailingZeros().toPlainString()
    }
}

/**
 * 将占比格式化为一位小数百分数。
 */
private fun formatPercentage(fraction: Float): String {
    val formatter = NumberFormat.getPercentInstance(Locale.CHINA)
    formatter.minimumFractionDigits = 1
    formatter.maximumFractionDigits = 1
    return formatter.format(fraction)
}

private val WEEKDAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
