package com.vos.accounting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
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
    transactions: List<TransactionRecord>,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val accountTransactions = transactions.filter { it.accountId == account.id }
    val income = accountTransactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::amountMinor)
    val expense = accountTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf(TransactionRecord::amountMinor)
    val balance = account.openingBalanceMinor + income - expense
    val monthlyTransactions = accountTransactions
        .groupBy(::accountRecordMonth)
        .entries
        .sortedByDescending(Map.Entry<YearMonth, List<TransactionRecord>>::key)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = account.name,
                modifier = Modifier.accountingBarBlur(backdrop),
                color = Color.Transparent,
                navigationIcon = {
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
                },
                actions = {
                    IconButton(
                        onClick = { onEditAccount(account.id) },
                        minWidth = 35.dp,
                        minHeight = 35.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Edit,
                            contentDescription = "编辑账户",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
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
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    AccountBalanceCard(
                        balance = balance,
                        income = income,
                        expense = expense,
                    )
                }
                if (monthlyTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            Text(
                                text = "暂无账目",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    monthlyTransactions.forEach { entry ->
                        item(key = entry.key.toString()) {
                            AccountMonthCard(
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

/**
 * 展示账户余额与累计流入流出摘要。
 */
@Composable
private fun AccountBalanceCard(
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
                .background(
                    Brush.linearGradient(
                        listOf(
                            MiuixTheme.colorScheme.primary.copy(alpha = 0.88f),
                            MiuixTheme.colorScheme.primaryContainer,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Text(
                text = "人民币余额",
                color = Color.White.copy(alpha = 0.84f),
                style = MiuixTheme.textStyles.body2,
            )
            Text(
                text = formatDecimalAmount(balance),
                modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.title1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Text(
                    text = "流入  ${formatDecimalAmount(income)}",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = "流出  ${formatDecimalAmount(expense)}",
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
    onEditTransaction: (Long) -> Unit,
) {
    val income = records
        .filter { it.type == TransactionType.INCOME }
        .sumOf(TransactionRecord::amountMinor)
    val expense = records
        .filter { it.type == TransactionType.EXPENSE }
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
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = month.format(ACCOUNT_MONTH_FORMATTER),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = "流入 ${formatDecimalAmount(income)}  流出 ${formatDecimalAmount(expense)}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.primary.copy(alpha = 0.10f),
                    cornerRadius = 13.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = accountingCategoryIcon(
                    iconKey = record.categoryIconKey,
                    fallbackName = record.categoryName,
                ),
                contentDescription = null,
                modifier = Modifier.size(25.dp),
                tint = MiuixTheme.colorScheme.primary,
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
            text = if (record.type == TransactionType.EXPENSE) {
                "-${formatDecimalAmount(record.amountMinor)}"
            } else {
                "+${formatDecimalAmount(record.amountMinor)}"
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
