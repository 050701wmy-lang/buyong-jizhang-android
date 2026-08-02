package com.vos.accounting.ui

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
import com.vos.accounting.model.TransactionType
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
) : AccountingRoute

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
                            onOpenManualEntry = { backStack.add(ManualEntryRoute()) },
                            onOpenTransactionEdit = { backStack.add(TransactionEditRoute(it)) },
                            onOpenAccount = { backStack.add(AccountDetailRoute(it)) },
                            onAddAccount = { backStack.add(AccountEditorRoute()) },
                            onThemeModeChange = {
                                viewModel.updateSettings(it, uiState.followSystemColor)
                            },
                            onFollowSystemColorChange = {
                                viewModel.updateSettings(uiState.themeMode, it)
                            },
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
                        if (account != null) {
                            AccountDetailScreen(
                                account = account,
                                transactions = uiState.transactions,
                                backdrop = backdrop,
                                onBack = { backStack.removeAt(backStack.lastIndex) },
                                onEditAccount = { backStack.add(AccountEditorRoute(it)) },
                                onEditTransaction = { backStack.add(TransactionEditRoute(it)) },
                                onAddTransaction = { backStack.add(ManualEntryRoute(account.id)) },
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
                            currentBalanceMinor = currentBalanceMinor,
                            writeInProgress = uiState.writeInProgress,
                            backdrop = backdrop,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onSave = viewModel::saveAccount,
                            onArchive = viewModel::archiveAccount,
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
            AccountingWriteErrorDialog(
                message = uiState.writeError,
                onDismiss = viewModel::clearWriteError,
            )
        }
    }
}
