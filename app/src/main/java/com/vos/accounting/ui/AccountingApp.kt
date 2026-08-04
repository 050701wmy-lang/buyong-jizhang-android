package com.vos.accounting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import com.vos.accounting.AccountingApplication
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionType
import com.vos.accounting.data.LedgerEntity
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 表示应用的 Navigation 3 页面键。
 */
@Serializable
sealed interface AccountingRoute : NavKey

/**
 * 表示包含四个主标签的首页入口。
 */
@Serializable
data object MainRoute : AccountingRoute

/**
 * 表示手动记账二级页面。
 */
@Serializable
data class ManualEntryRoute(
    val accountId: Long = 0,
) : AccountingRoute

/**
 * 表示编辑指定账目的二级页面。
 */
@Serializable
data class TransactionEditRoute(
    val transactionId: Long,
) : AccountingRoute

/**
 * 表示指定账户的详情页面。
 */
@Serializable
data class AccountDetailRoute(
    val accountId: Long,
) : AccountingRoute

/**
 * 表示新增或编辑账户的页面。
 */
@Serializable
data class AccountEditorRoute(
    val accountId: Long = 0,
    val iconKey: String? = null,
    val typeKey: String? = null,
    val currencyKey: String? = null,
    val ledgerIds: List<Long>? = null,
) : AccountingRoute

/** 表示账本选择二级页面。 */
@Serializable
data object LedgerRoute : AccountingRoute

/** 表示新增或编辑账本的二级页面。 */
@Serializable
data class LedgerEditorRoute(val ledgerId: Long = 0) : AccountingRoute

/** 表示账户适用账本多选页面。 */
@Serializable
data class AccountLedgerPickerRoute(val selectedIds: List<Long>) : AccountingRoute

/**
 * 表示账户币种选择二级页面。
 */
@Serializable
data class CurrencyRoute(
    val selectedKey: String,
) : AccountingRoute

/**
 * 表示账户类型选择二级页面。
 */
@Serializable
data class AccountTypeRoute(
    val selectedKey: String,
) : AccountingRoute

/**
 * 表示账户图标选择二级页面。
 */
@Serializable
data class AccountIconRoute(
    val selectedKey: String,
) : AccountingRoute

/**
 * 表示应用设置二级页面。
 */
@Serializable
data object SettingsRoute : AccountingRoute

/**
 * 表示 AI 智能记账二级页面。
 */
@Serializable
data object AiEntryRoute : AccountingRoute

/**
 * 建立 MIUIX 主题、共享模糊内容层与 Navigation 3 页面栈。
 */
@Composable
fun AccountingApp() {
    val application = LocalContext.current.applicationContext as AccountingApplication
    val viewModel: AccountingViewModel = viewModel(
        factory = AccountingViewModel.factory(application.repository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(MainRoute)
    val navigateTo = remember(backStack) {
        { route: AccountingRoute ->
            if (backStack.lastOrNull() != route) {
                backStack.add(route)
            }
        }
    }

    // 统一管理页面转场：新页从右侧进入、来源页左退压暗、预测返回跟随手势。
    val transitionEffects = remember {
        NavDisplayTransitionEffects(
            enableCornerClip = true,
            dimAmount = 0.5f,
            blockInputDuringTransition = true,
            popDirectionFollowsSwipeEdge = true,
        )
    }

    AccountingTheme(
        themeMode = uiState.themeMode,
        followSystemColor = uiState.followSystemColor,
    ) {
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            NavDisplay(
                transitionEffects = transitionEffects,
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                },
                entryProvider = entryProvider {
                    entry<MainRoute> {
                        MainShell(
                            uiState = uiState,
                            backdrop = backdrop,
                            onOpenManualEntry = { navigateTo(ManualEntryRoute()) },
                            onOpenTransactionEdit = { navigateTo(TransactionEditRoute(it)) },
                            onOpenAccount = { navigateTo(AccountDetailRoute(it)) },
                            onAddAccount = { navigateTo(AccountEditorRoute()) },
                            onOpenSettings = { navigateTo(SettingsRoute) },
                            onOpenLedgers = { navigateTo(LedgerRoute) },
                            onSelectLedger = { ledgerId -> viewModel.selectLedger(ledgerId) {} },
                        )
                    }
                    entry<ManualEntryRoute> { route ->
                        ManualEntryScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            initialAccountId = route.accountId,
                            onSave = viewModel::saveTransaction,
                            onAddCategory = viewModel::addCategory,
                        )
                    }
                    entry<TransactionEditRoute> { route ->
                        val transaction = uiState.transactions.firstOrNull {
                            it.id == route.transactionId
                        }
                        if (transaction != null) {
                            ManualEntryScreen(
                                uiState = uiState,
                                backdrop = backdrop,
                                onBack = { backStack.removeAt(backStack.lastIndex) },
                                transaction = transaction,
                                onSave = viewModel::saveTransaction,
                                onUpdate = viewModel::updateTransaction,
                                onDelete = viewModel::deleteTransaction,
                                onAddCategory = viewModel::addCategory,
                            )
                        }
                    }
                    entry<AccountDetailRoute> { route ->
                        val account = uiState.accounts.firstOrNull { it.id == route.accountId }
                        val currency = uiState.currencies.firstOrNull { it.key == account?.currencyKey }
                        if (account != null && currency != null) {
                            AccountDetailScreen(
                                account = account,
                                currency = currency,
                                transactions = uiState.transactions,
                                backdrop = backdrop,
                                onBack = { backStack.removeAt(backStack.lastIndex) },
                                onEditAccount = { navigateTo(AccountEditorRoute(it)) },
                                onEditTransaction = { navigateTo(TransactionEditRoute(it)) },
                                onAddTransaction = { navigateTo(ManualEntryRoute(account.id)) },
                            )
                        }
                    }
                    entry<AccountEditorRoute> { route ->
                        val account = uiState.accounts.firstOrNull { it.id == route.accountId }
                        val accountTransactions = uiState.transactions.filter {
                            it.accountId == route.accountId
                        }
                        val currentBalanceMinor = if (account == null) {
                            0
                        } else {
                            account.openingBalanceMinor + accountTransactions.sumOf {
                                if (it.type == TransactionType.INCOME) {
                                    it.amountMinor
                                } else {
                                    -it.amountMinor
                                }
                            }
                        }
                        AccountEditorScreen(
                            account = account,
                            accountTypes = uiState.accountTypes,
                            currencies = uiState.currencies,
                            selectedTypeKey = route.typeKey ?: account?.typeKey ?: "cash",
                            selectedCurrencyKey = route.currencyKey ?: account?.currencyKey ?: "cny",
                            selectedIconKey = route.iconKey ?: account?.iconKey
                                ?: defaultAccountIconKey(AccountType.CASH),
                            ledgers = uiState.ledgers,
                            selectedLedgerIds = route.ledgerIds?.toSet()
                                ?: uiState.accountLedgerCrossRefs.filter { it.accountId == account?.id }.map { it.ledgerId }.toSet()
                                    .ifEmpty { setOf(uiState.currentLedgerId) },
                            currentBalanceMinor = currentBalanceMinor,
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onSave = viewModel::saveAccount,
                            onArchive = viewModel::archiveAccount,
                            onOpenTypePicker = { navigateTo(AccountTypeRoute(it)) },
                            onOpenCurrencyPicker = { navigateTo(CurrencyRoute(it)) },
                            onOpenIconPicker = { navigateTo(AccountIconRoute(it)) },
                            onOpenLedgerPicker = { navigateTo(AccountLedgerPickerRoute(it.toList())) },
                        )
                    }
                    entry<AccountTypeRoute> { route ->
                        AccountTypeScreen(
                            accountTypes = uiState.accountTypes,
                            selectedKey = route.selectedKey,
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onSelect = { typeKey ->
                                val editorIndex = backStack.lastIndex - 1
                                val editor = backStack[editorIndex] as AccountEditorRoute
                                backStack[editorIndex] = editor.copy(typeKey = typeKey)
                                backStack.removeAt(backStack.lastIndex)
                            },
                            onAdd = viewModel::addAccountType,
                            onUpdate = viewModel::updateAccountType,
                            onDelete = viewModel::deleteAccountType,
                        )
                    }
                    entry<CurrencyRoute> { route ->
                        CurrencyScreen(
                            currencies = uiState.currencies,
                            selectedKey = route.selectedKey,
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onSelect = { currencyKey ->
                                val editorIndex = backStack.lastIndex - 1
                                val editor = backStack[editorIndex] as AccountEditorRoute
                                backStack[editorIndex] = editor.copy(currencyKey = currencyKey)
                                backStack.removeAt(backStack.lastIndex)
                            },
                            onAdd = viewModel::addCurrency,
                            onUpdate = viewModel::updateCurrency,
                            onDelete = viewModel::deleteCurrency,
                            onAutoRateChange = viewModel::setCurrencyAutoRate,
                        )
                    }
                    entry<AccountIconRoute> { route ->
                        AccountIconScreen(
                            selectedKey = route.selectedKey,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onSelect = { iconKey ->
                                val editorIndex = backStack.lastIndex - 1
                                val editor = backStack[editorIndex] as AccountEditorRoute
                                backStack[editorIndex] = editor.copy(iconKey = iconKey)
                                backStack.removeAt(backStack.lastIndex)
                            },
                        )
                    }
                    entry<LedgerRoute> {
                        LedgerScreen(
                            ledgers = uiState.ledgers,
                            currentLedgerId = uiState.currentLedgerId,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onAdd = { navigateTo(LedgerEditorRoute()) },
                            onEdit = { navigateTo(LedgerEditorRoute(it)) },
                            onSelect = { id -> viewModel.selectLedger(id) { backStack.removeAt(backStack.lastIndex) } },
                        )
                    }
                    entry<LedgerEditorRoute> { route ->
                        val record = uiState.ledgers.firstOrNull { it.id == route.ledgerId }
                        val ledger = record?.let {
                            LedgerEntity(it.id, it.name, it.coverKey, it.useLightText, it.baseCurrencyKey, it.isHidden, it.sortOrder)
                        }
                        LedgerEditorScreen(
                            ledger = ledger,
                            currencies = uiState.currencies,
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onAdd = viewModel::addLedger,
                            onUpdate = viewModel::updateLedger,
                            onDelete = viewModel::deleteLedger,
                        )
                    }
                    entry<AccountLedgerPickerRoute> { route ->
                        AccountLedgerPickerScreen(
                            ledgers = uiState.ledgers,
                            selectedIds = route.selectedIds.toSet(),
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onConfirm = { ids ->
                                val editorIndex = backStack.lastIndex - 1
                                val editor = backStack[editorIndex] as AccountEditorRoute
                                backStack[editorIndex] = editor.copy(ledgerIds = ids.toList())
                                backStack.removeAt(backStack.lastIndex)
                            },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onThemeModeChange = viewModel::updateThemeMode,
                            onFollowSystemColorChange = viewModel::updateFollowSystemColor,
                            onPredictiveBackAnimationEnabledChange =
                                viewModel::updatePredictiveBackAnimationEnabled,
                        )
                    }
                    entry<AiEntryRoute> {
                        AiEntryScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = {
                                viewModel.clearAiDraft()
                                backStack.removeAt(backStack.lastIndex)
                            },
                            onCreateDraft = viewModel::createAiDraft,
                            onSave = viewModel::saveTransaction,
                        )
                    }
                },
            )
            BackHandler(
                enabled = !uiState.predictiveBackAnimationEnabled && backStack.size > 1,
                onBack = { backStack.removeAt(backStack.lastIndex) },
            )
            AccountingWriteErrorDialog(
                message = uiState.writeError,
                onDismiss = viewModel::clearWriteError,
            )
        }
    }
}
