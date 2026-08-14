package com.vos.accounting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import com.vos.accounting.auto.AutoBookkeepingNotificationManager
import com.vos.accounting.auto.toEditableTransactionDraft
import com.vos.accounting.model.AccountType
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
data class AccountLedgerPickerRoute(val accountId: Long) : AccountingRoute

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

/** 表示数据备份与恢复二级页面。 */
@Serializable
data object BackupRoute : AccountingRoute

/** 表示 AI 记账设置二级页面。 */
@Serializable
data object AutoBookkeepingSettingsRoute : AccountingRoute

/** 表示声明式识别规则管理二级页面。 */
@Serializable
data object AutoRuleManagementRoute : AccountingRoute

/** 表示全部待确认自动账单列表。 */
@Serializable
data object AutoBookkeepingPendingRoute : AccountingRoute

/** 表示指定待确认自动账单的编辑页面。 */
@Serializable
data class AutoBookkeepingEventRoute(val eventId: Long) : AccountingRoute

/**
 * 建立 MIUIX 主题、共享模糊内容层与 Navigation 3 页面栈。
 */
@Composable
fun AccountingApp(
    initialAutoBookkeepingEventId: Long? = null,
    onInitialAutoBookkeepingEventConsumed: () -> Unit = {},
) {
    val application = LocalContext.current.applicationContext as AccountingApplication
    val viewModel: AccountingViewModel = viewModel(
        factory = AccountingViewModel.factory(application.repository, application.backupManager),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(MainRoute)
    val accountLedgerSelections = remember { mutableStateMapOf<Long, Set<Long>>() }
    val accountEditorBackRequests = remember { mutableMapOf<Long, () -> Unit>() }
    val autoNotificationManager = remember(application) {
        AutoBookkeepingNotificationManager(application, application.repository)
    }
    val navigateTo = remember(backStack) {
        { route: AccountingRoute ->
            if (backStack.lastOrNull() != route) {
                backStack.add(route)
            }
        }
    }
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) {
            val route = backStack.last()
            val request = (route as? AccountEditorRoute)
                ?.let { accountEditorBackRequests[it.accountId] }
            if (request == null) {
                backStack.removeAt(backStack.lastIndex)
            } else {
                request()
            }
        }
    }
    LaunchedEffect(initialAutoBookkeepingEventId) {
        initialAutoBookkeepingEventId?.takeIf { it > 0 }?.let { eventId ->
            navigateTo(AutoBookkeepingEventRoute(eventId))
            onInitialAutoBookkeepingEventConsumed()
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
                onBack = navigateBack,
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
                            onOpenPendingAutoBookkeeping = { navigateTo(AutoBookkeepingPendingRoute) },
                            onOpenLedgers = { navigateTo(LedgerRoute) },
                            onSelectLedger = { ledgerId -> viewModel.selectLedger(ledgerId) {} },
                        )
                    }
                    entry<ManualEntryRoute> { route ->
                        ManualEntryScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = {
                                if (backStack.lastOrNull() == route) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            },
                            initialAccountId = route.accountId,
                            onSave = viewModel::saveTransaction,
                            onAddLedger = { navigateTo(LedgerEditorRoute()) },
                            onManageLedgers = { navigateTo(LedgerRoute) },
                            onAddAccount = { ledgerId ->
                                navigateTo(AccountEditorRoute(ledgerIds = listOf(ledgerId)))
                            },
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
                                onBack = {
                                    if (backStack.lastOrNull() == route) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                },
                                transaction = transaction,
                                onSave = viewModel::saveTransaction,
                                onUpdate = viewModel::updateTransaction,
                                onDelete = viewModel::deleteTransaction,
                                onAddLedger = { navigateTo(LedgerEditorRoute()) },
                                onManageLedgers = { navigateTo(LedgerRoute) },
                                onAddAccount = { ledgerId ->
                                    navigateTo(AccountEditorRoute(ledgerIds = listOf(ledgerId)))
                                },
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
                                accountBalanceMinor = uiState.accountBalances[account.id]?.balanceMinor
                                    ?: account.openingBalanceMinor,
                                incomeMinor = uiState.accountBalances[account.id]?.incomeMinor ?: 0,
                                expenseMinor = uiState.accountBalances[account.id]?.expenseMinor ?: 0,
                                accountExchangeLinks = uiState.accountExchangeLinks,
                                coloredTransactionAmountsEnabled = uiState.coloredTransactionAmountsEnabled,
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
                        val storedLedgerIds = route.ledgerIds?.toSet()
                            ?: uiState.accountLedgerCrossRefs
                                .filter { it.accountId == account?.id }
                                .map { it.ledgerId }
                                .toSet()
                                .ifEmpty { setOf(uiState.currentLedgerId) }
                        val selectedLedgerIds = accountLedgerSelections[route.accountId] ?: storedLedgerIds
                        val closeEditor = {
                            accountLedgerSelections.remove(route.accountId)
                            accountEditorBackRequests.remove(route.accountId)
                            if (backStack.lastOrNull() == route) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            Unit
                        }
                        val currentBalanceMinor = account?.let {
                            uiState.accountBalances[it.id]?.balanceMinor ?: it.openingBalanceMinor
                        } ?: 0
                        AccountEditorScreen(
                            account = account,
                            accountTypes = uiState.accountTypes,
                            currencies = uiState.currencies,
                            selectedTypeKey = route.typeKey ?: account?.typeKey ?: "cash",
                            selectedCurrencyKey = route.currencyKey ?: account?.currencyKey ?: "cny",
                            selectedIconKey = route.iconKey ?: account?.iconKey
                                ?: defaultAccountIconKey(AccountType.CASH),
                            ledgers = uiState.ledgers,
                            selectedLedgerIds = selectedLedgerIds,
                            currentBalanceMinor = currentBalanceMinor,
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = closeEditor,
                            onBackRequestChange = { request ->
                                if (request == null) {
                                    accountEditorBackRequests.remove(route.accountId)
                                } else {
                                    accountEditorBackRequests[route.accountId] = request
                                }
                            },
                            onSave = viewModel::saveAccount,
                            onDelete = viewModel::deleteAccount,
                            onOpenTypePicker = { navigateTo(AccountTypeRoute(it)) },
                            onOpenCurrencyPicker = { navigateTo(CurrencyRoute(it)) },
                            onOpenIconPicker = { navigateTo(AccountIconRoute(it)) },
                            onOpenLedgerPicker = { ids ->
                                accountLedgerSelections[route.accountId] = ids
                                navigateTo(AccountLedgerPickerRoute(route.accountId))
                            },
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
                    entry<LedgerRoute> { route ->
                        LedgerScreen(
                            ledgers = uiState.ledgers,
                            currentLedgerId = uiState.currentLedgerId,
                            backdrop = backdrop,
                            onBack = {
                                if (backStack.lastOrNull() == route) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            },
                            onAdd = { navigateTo(LedgerEditorRoute()) },
                            onEdit = { navigateTo(LedgerEditorRoute(it)) },
                            onSelect = { id ->
                                viewModel.selectLedger(id) {
                                    if (backStack.lastOrNull() == route) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            },
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
                            onBack = {
                                if (backStack.lastOrNull() == route) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            },
                            onAdd = viewModel::addLedger,
                            onUpdate = viewModel::updateLedger,
                            onDelete = viewModel::deleteLedger,
                        )
                    }
                    entry<AccountLedgerPickerRoute> { route ->
                        val selectedLedgerIds = accountLedgerSelections[route.accountId]
                            ?: uiState.accountLedgerCrossRefs
                                .filter { it.accountId == route.accountId }
                                .map { it.ledgerId }
                                .toSet()
                                .ifEmpty { setOf(uiState.currentLedgerId) }
                        AccountLedgerPickerScreen(
                            ledgers = uiState.ledgers,
                            selectedIds = selectedLedgerIds,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onSelectionChange = { ids -> accountLedgerSelections[route.accountId] = ids },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onOpenAutoBookkeeping = { navigateTo(AutoBookkeepingSettingsRoute) },
                            onOpenBackup = { navigateTo(BackupRoute) },
                            onThemeModeChange = viewModel::updateThemeMode,
                            onFollowSystemColorChange = viewModel::updateFollowSystemColor,
                            onPredictiveBackAnimationEnabledChange =
                                viewModel::updatePredictiveBackAnimationEnabled,
                            onColoredTransactionAmountsEnabledChange =
                                viewModel::updateColoredTransactionAmountsEnabled,
                            onXiaomiSuperIslandEnabledChange =
                                viewModel::updateXiaomiSuperIslandEnabled,
                        )
                    }
                    entry<AutoBookkeepingSettingsRoute> {
                        AutoBookkeepingSettingsScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onOpenRuleManagement = { navigateTo(AutoRuleManagementRoute) },
                            viewModel = viewModel,
                        )
                    }
                    entry<AutoRuleManagementRoute> {
                        AutoRuleManagementScreen(
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            viewModel = viewModel,
                        )
                    }
                    entry<AutoBookkeepingPendingRoute> {
                        AutoBookkeepingPendingScreen(
                            uiState = uiState,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onEdit = { navigateTo(AutoBookkeepingEventRoute(it)) },
                            onConfirm = { eventId ->
                                viewModel.confirmAutoBookkeepingEvent(eventId) {
                                    autoNotificationManager.cancel(eventId)
                                }
                            },
                            onIgnore = { eventId ->
                                viewModel.ignoreAutoBookkeepingEvent(eventId) {
                                    autoNotificationManager.cancel(eventId)
                                }
                            },
                        )
                    }
                    entry<AutoBookkeepingEventRoute> { route ->
                        val event = uiState.pendingAutoBookkeepingEvents.firstOrNull { it.id == route.eventId }
                        if (event == null) {
                            AutoBookkeepingPendingScreen(
                                uiState = uiState,
                                backdrop = backdrop,
                                onBack = { backStack.removeAt(backStack.lastIndex) },
                                onEdit = { navigateTo(AutoBookkeepingEventRoute(it)) },
                                onConfirm = viewModel::confirmAutoBookkeepingEvent,
                                onIgnore = viewModel::ignoreAutoBookkeepingEvent,
                            )
                        } else {
                            ManualEntryScreen(
                                uiState = uiState,
                                backdrop = backdrop,
                                onBack = {
                                    if (backStack.lastOrNull() == route) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                },
                                initialDraft = event.toEditableTransactionDraft(),
                                draftKey = event.id,
                                allowLedgerChange = false,
                                onSave = { draft, onSaved ->
                                    viewModel.confirmAutoBookkeepingEvent(event.id, draft) {
                                        autoNotificationManager.cancel(event.id)
                                        onSaved()
                                    }
                                },
                                onAddLedger = { navigateTo(LedgerEditorRoute()) },
                                onManageLedgers = { navigateTo(LedgerRoute) },
                                onAddAccount = { ledgerId ->
                                    navigateTo(AccountEditorRoute(ledgerIds = listOf(ledgerId)))
                                },
                                onAddCategory = viewModel::addCategory,
                            )
                        }
                    }
                    entry<BackupRoute> {
                        BackupRestoreScreen(
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = {
                                if (backStack.lastOrNull() == BackupRoute) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            },
                            onExport = viewModel::exportBackup,
                            onParse = viewModel::parseBackup,
                            onApply = viewModel::applyBackup,
                        )
                    }
                },
            )
            BackHandler(
                enabled = !uiState.predictiveBackAnimationEnabled && backStack.size > 1,
                onBack = navigateBack,
            )
            AccountingWriteErrorDialog(
                message = uiState.writeError,
                onDismiss = viewModel::clearWriteError,
            )
        }
    }
}
