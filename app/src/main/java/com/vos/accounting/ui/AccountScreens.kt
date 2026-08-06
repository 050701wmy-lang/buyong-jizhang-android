package com.vos.accounting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransferDirection
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 展示指定账户的真实余额、累计收支和按月账目。
 */
@Composable
fun AccountDetailScreen(
    account: AccountEntity,
    currency: CurrencyEntity,
    transactions: List<TransactionRecord>,
    balanceTransactions: List<TransactionRecord>,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val accountTransactions = transactions.filter { it.accountId == account.id }
    val accountBalanceTransactions = balanceTransactions.filter { it.accountId == account.id }
    val income = accountBalanceTransactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::amountMinor)
    val expense = accountBalanceTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf(TransactionRecord::amountMinor)
    val transferIn = accountBalanceTransactions
        .filter { it.type == TransactionType.TRANSFER && it.transferDirection == TransferDirection.IN }
        .sumOf(TransactionRecord::amountMinor)
    val transferOut = accountBalanceTransactions
        .filter { it.type == TransactionType.TRANSFER && it.transferDirection == TransferDirection.OUT }
        .sumOf(TransactionRecord::amountMinor)
    val balance = account.openingBalanceMinor + income + transferIn - expense - transferOut
    val monthlyTransactions = accountTransactions
        .groupBy(::accountRecordMonth)
        .entries
        .sortedByDescending(Map.Entry<YearMonth, List<TransactionRecord>>::key)

    val scrollBehavior = MiuixScrollBehavior()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AccountDetailTopBar(
                    title = account.name,
                    isWide = isWide,
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    onBack = onBack,
                    onEdit = { onEditAccount(account.id) },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    modifier = if (isWide) {
                        Modifier
                    } else {
                        Modifier.offset(x = (-22.5f).dp, y = (-21.5f).dp)
                    },
                    containerColor = MiuixTheme.colorScheme.primaryContainer,
                    minWidth = 56.dp,
                    minHeight = 56.dp,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = "使用当前账户记一笔",
                        tint = Color.White,
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
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
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        AccountBalanceCard(
                            currency = currency,
                            balance = balance,
                            income = income,
                            expense = expense,
                        )
                    }
                    if (monthlyTransactions.isEmpty()) {
                        item {
                            EmptyCard(text = "暂无账目")
                        }
                    } else {
                        monthlyTransactions.forEach { entry ->
                            item(key = entry.key.toString()) {
                                AccountMonthCard(
                                    currencySymbol = currency.symbol,
                                    month = entry.key,
                                    records = entry.value,
                                    onEditTransaction = onEditTransaction,
                                )
                            }
                        }
                    }
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(96.dp)
                                .navigationBarsPadding(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 根据窗口宽度展示可折叠大标题或固定小标题账户顶栏。
 */
@Composable
private fun AccountDetailTopBar(
    title: String,
    isWide: Boolean,
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val navigationIcon: @Composable () -> Unit = {
        IconButton(
            onClick = onBack,
            minWidth = 35.dp,
            minHeight = 35.dp,
        ) {
            Icon(
                imageVector = MiuixIcons.Back,
                contentDescription = "返回",
            )
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        IconButton(
            onClick = onEdit,
            backgroundColor = Color.Transparent,
            minWidth = TOP_BAR_ACTION_BUTTON_SIZE,
            minHeight = TOP_BAR_ACTION_BUTTON_SIZE,
        ) {
            Icon(
                imageVector = MiuixIcons.Edit,
                contentDescription = "编辑账户",
                modifier = Modifier.size(TOP_BAR_ACTION_ICON_SIZE),
            )
        }
    }
    AccountingBlurTopBar(backdrop = backdrop) {
        if (isWide) {
            SmallTopAppBar(
                title = title,
                color = Color.Transparent,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                actionIconPadding = TOP_BAR_ACTION_END_PADDING,
            )
        } else {
            TopAppBar(
                title = title,
                color = Color.Transparent,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                actionIconPadding = TOP_BAR_ACTION_END_PADDING,
            )
        }
    }
}

/**
 * 展示账户余额与累计流入流出摘要。
 */
@Composable
private fun AccountBalanceCard(
    currency: CurrencyEntity,
    balance: Long,
    income: Long,
    expense: Long,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(LEDGER_HERO_ASPECT_RATIO)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MiuixTheme.colorScheme.primary.copy(alpha = 0.88f),
                            MiuixTheme.colorScheme.primaryContainer,
                        ),
                    ),
                )
                .padding(HERO_CARD_CONTENT_PADDING),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "${currency.name}余额",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = formatCurrencyAmount(balance, currency.symbol),
                    modifier = Modifier.padding(top = 5.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MiuixTheme.textStyles.title1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Text(
                    text = "流入  ${formatCurrencyAmount(income, currency.symbol)}",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = "流出  ${formatCurrencyAmount(expense, currency.symbol)}",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

/**
 * 以一个连续 Card 展示同月小计和账目。
 */
@Composable
private fun AccountMonthCard(
    month: YearMonth,
    records: List<TransactionRecord>,
    currencySymbol: String,
    onEditTransaction: (Long) -> Unit,
) {
    val inflow = records
        .filter {
            it.type == TransactionType.INCOME ||
                (it.type == TransactionType.TRANSFER && it.transferDirection == TransferDirection.IN)
        }
        .sumOf(TransactionRecord::amountMinor)
    val outflow = records
        .filter {
            it.type == TransactionType.EXPENSE ||
                (it.type == TransactionType.TRANSFER && it.transferDirection == TransferDirection.OUT)
        }
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = month.format(ACCOUNT_MONTH_FORMATTER),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = "流入 ${formatCurrencyAmount(inflow, currencySymbol)}  流出 ${formatCurrencyAmount(outflow, currencySymbol)}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = GROUPED_CARD_HORIZONTAL_PADDING))
        records.forEach { record ->
            AccountTransactionRow(
                record = record,
                onClick = { onEditTransaction(record.id) },
            )
        }
    }
}

/**
 * 展示账户详情中的一笔可编辑账目。
 */
@Composable
private fun AccountTransactionRow(
    record: TransactionRecord,
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
            val iconOption = if (isTransfer) {
                null
            } else {
                categoryIconOption(record.categoryIconKey, record.categoryName)
            }
            Icon(
                imageVector = if (isTransfer) {
                    MiuixIcons.Basic.ArrowUpDown
                } else {
                    iconOption!!.icon
                },
                contentDescription = null,
                modifier = Modifier.size(GROUPED_CARD_ICON_SIZE),
                tint = if (iconOption?.colorful == true) {
                    Color.Unspecified
                } else {
                    MiuixTheme.colorScheme.primary
                },
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
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = accountRecordTime(record.occurredAt),
                modifier = Modifier.padding(top = 3.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        Text(
            text = if (
                record.type == TransactionType.EXPENSE ||
                (record.type == TransactionType.TRANSFER && record.transferDirection == TransferDirection.OUT)
            ) {
                "-${formatCurrencyAmount(record.amountMinor, record.currencySymbol)}"
            } else {
                "+${formatCurrencyAmount(record.amountMinor, record.currencySymbol)}"
            },
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

/**
 * 返回账目在设备时区对应的月份。
 */
private fun accountRecordMonth(record: TransactionRecord): YearMonth = YearMonth.from(
    Instant.ofEpochMilli(record.occurredAt).atZone(ZoneId.systemDefault()),
)

/**
 * 格式化账户详情中的账目时间。
 */
private fun accountRecordTime(timestamp: Long): String = Instant
    .ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(ACCOUNT_TIME_FORMATTER)

private val ACCOUNT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM", Locale.CHINA)
private val ACCOUNT_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA)
