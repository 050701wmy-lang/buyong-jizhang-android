package com.vos.accounting.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 顶栏右侧操作按钮的最小点击尺寸。 */
internal val TOP_BAR_ACTION_BUTTON_SIZE = 35.dp

/** 顶栏右侧操作图标的统一尺寸。 */
internal val TOP_BAR_ACTION_ICON_SIZE = 24.dp

/** 顶栏右侧操作按钮到屏幕边缘的留白。 */
internal val TOP_BAR_ACTION_END_PADDING = 20.dp

/**
 * 表示四个一级功能标签。
 */
enum class MainTab(
    val title: String,
    val icon: ImageVector,
) {
    HOME("首页", MiuixIcons.Home),
    DETAILS("明细", MiuixIcons.ListView),
    STATISTICS("报表", MiuixIcons.GridView),
    SETTINGS("设置", MiuixIcons.Settings),
    ;

    /**
     * 返回页面顶栏使用的完整标题。
     */
    val topBarTitle: String
        get() = if (this == STATISTICS) "收支报表" else title
}

/**
 * 根据窗口宽度组织底部导航或可展开侧边导航。
 */
@Composable
fun MainShell(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onOpenManualEntry: () -> Unit,
    onOpenTransactionEdit: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        if (isWide) {
            val navigationRailState = rememberNavigationRailState()
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    state = navigationRailState,
                    showDivider = false,
                ) {
                    MainTab.entries.filterNot { it == MainTab.SETTINGS }.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = tab.icon,
                            label = tab.title,
                        )
                    }
                }
                MainScaffold(
                    modifier = Modifier.weight(1f),
                    uiState = uiState,
                    selectedTab = selectedTab,
                    isWide = true,
                    backdrop = backdrop,
                    onSelectTab = { selectedTab = it },
                    onOpenManualEntry = onOpenManualEntry,
                    onOpenTransactionEdit = onOpenTransactionEdit,
                    onAddAccount = onAddAccount,
                    onOpenAccount = onOpenAccount,
                )
            }
        } else {
            MainScaffold(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                selectedTab = selectedTab,
                isWide = false,
                backdrop = backdrop,
                onSelectTab = { selectedTab = it },
                onOpenManualEntry = onOpenManualEntry,
                onOpenTransactionEdit = onOpenTransactionEdit,
                onAddAccount = onAddAccount,
                onOpenAccount = onOpenAccount,
            )
        }
    }
}

/**
 * 以一个共享 Scaffold 承担主标签的顶栏、底栏和浮动操作。
 */
@Composable
private fun MainScaffold(
    modifier: Modifier,
    uiState: AccountingUiState,
    selectedTab: MainTab,
    isWide: Boolean,
    backdrop: LayerBackdrop,
    onSelectTab: (MainTab) -> Unit,
    onOpenManualEntry: () -> Unit,
    onOpenTransactionEdit: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onOpenAccount: (Long) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AccountingBlurTopBar(backdrop = backdrop) {
                if (isWide) {
                    SmallTopAppBar(
                        title = selectedTab.topBarTitle,
                        color = Color.Transparent,
                        scrollBehavior = scrollBehavior,
                        actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                        actions = {
                            MainSettingsAction(onClick = { onSelectTab(MainTab.SETTINGS) })
                        },
                    )
                } else {
                    TopAppBar(
                        title = selectedTab.topBarTitle,
                        color = Color.Transparent,
                        scrollBehavior = scrollBehavior,
                        actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                        actions = {
                            MainSettingsAction(onClick = { onSelectTab(MainTab.SETTINGS) })
                        },
                    )
                }
            }
        },
        bottomBar = {
            if (!isWide) {
                NavigationBar(
                    modifier = Modifier.accountingBarBlur(backdrop),
                    color = Color.Transparent,
                    showDivider = false,
                ) {
                    MainTab.entries.filterNot { it == MainTab.SETTINGS }.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            icon = tab.icon,
                            label = tab.title,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab != MainTab.SETTINGS) {
                FloatingActionButton(
                    onClick = if (selectedTab == MainTab.HOME) onAddAccount else onOpenManualEntry,
                    modifier = Modifier.offset(x = (-22.5f).dp, y = (-21.5f).dp),
                    containerColor = MiuixTheme.colorScheme.primaryContainer,
                    minWidth = 56.dp,
                    minHeight = 56.dp,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = if (selectedTab == MainTab.HOME) {
                            "新增账户"
                        } else {
                            "手动记账"
                        },
                        tint = Color.White,
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onOpenAccount = onOpenAccount,
                )

                MainTab.DETAILS -> DetailsScreen(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onEditTransaction = onOpenTransactionEdit,
                )

                MainTab.STATISTICS -> StatisticsScreen(
                    uiState = uiState,
                    innerPadding = innerPadding,
                )

                MainTab.SETTINGS -> SettingsScreen(innerPadding = innerPadding)
            }
        }
    }
}

/**
 * 在独立背景层绘制顶栏模糊，避免顶栏文字动画图层参与模糊合成。
 */
@Composable
internal fun AccountingBlurTopBar(
    backdrop: LayerBackdrop,
    content: @Composable () -> Unit,
) {
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .accountingBarBlur(backdrop),
        )
        content()
    }
}

/**
 * 显示主页面顶栏右侧的设置入口。
 */
@Composable
private fun MainSettingsAction(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        backgroundColor = Color.Transparent,
        minWidth = TOP_BAR_ACTION_BUTTON_SIZE,
        minHeight = TOP_BAR_ACTION_BUTTON_SIZE,
    ) {
        Icon(
            imageVector = MiuixIcons.Settings,
            contentDescription = "设置",
            modifier = Modifier.size(TOP_BAR_ACTION_ICON_SIZE),
        )
    }
}

/**
 * 为顶栏和底栏提供统一的 MIUIX Backdrop 模糊或不透明表面。
 */
@Composable
fun Modifier.accountingBarBlur(backdrop: LayerBackdrop): Modifier =
    if (isAccountingBlurSupported()) {
        textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 24f,
        )
    } else {
        background(MiuixTheme.colorScheme.surface)
    }

/**
 * 判断当前系统是否能稳定绘制 MIUIX RuntimeShader 模糊。
 */
private fun isAccountingBlurSupported(): Boolean =
    isRuntimeShaderSupported() && Build.VERSION.SDK_INT != Build.VERSION_CODES.BAKLAVA
