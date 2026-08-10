package com.vos.accounting.ui

import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
import top.yukonga.miuix.kmp.basic.Text
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

/** 相邻顶栏操作按钮之间的统一间距。 */
internal val TOP_BAR_ACTION_SPACING = 5.dp

/**
 * 展示顶栏右上角统一尺寸与透明背景的操作图标按钮。
 */
@Composable
internal fun TopBarIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        backgroundColor = Color.Transparent,
        minWidth = TOP_BAR_ACTION_BUTTON_SIZE,
        minHeight = TOP_BAR_ACTION_BUTTON_SIZE,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(TOP_BAR_ACTION_ICON_SIZE),
        )
    }
}

/**
 * 展示 MIUIX 分组小标题，供各页面分区标题共用。
 */
@Composable
internal fun AccountSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
    color: Color = MiuixTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        style = MiuixTheme.textStyles.body2,
    )
}

/**
 * 表示三个一级功能标签。
 */
enum class MainTab(
    val title: String,
    val icon: ImageVector,
) {
    HOME("首页", MiuixIcons.Home),
    DETAILS("明细", MiuixIcons.ListView),
    STATISTICS("报表", MiuixIcons.GridView),
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
    onOpenSettings: () -> Unit,
    onOpenLedgers: () -> Unit,
    onSelectLedger: (Long) -> Unit,
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
                    MainTab.entries.forEach { tab ->
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
                    onOpenSettings = onOpenSettings,
                    onOpenLedgers = onOpenLedgers,
                    onSelectLedger = onSelectLedger,
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
                onOpenSettings = onOpenSettings,
                onOpenLedgers = onOpenLedgers,
                onSelectLedger = onSelectLedger,
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
    onOpenSettings: () -> Unit,
    onOpenLedgers: () -> Unit,
    onSelectLedger: (Long) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var mainViewportWidth by remember { mutableIntStateOf(0) }
    val pagerState = if (isWide) {
        null
    } else {
        rememberPagerState(
            initialPage = selectedTab.ordinal,
            pageCount = { MainTab.entries.size },
        )
    }
    val pagerCoroutineScope = rememberCoroutineScope()
    val activeTab = pagerState?.let { MainTab.entries[it.currentPage] } ?: selectedTab
    if (pagerState != null) {
        LaunchedEffect(pagerState.currentPage) {
            onSelectTab(MainTab.entries[pagerState.currentPage])
        }
    }
    Scaffold(
        modifier = modifier
            .onSizeChanged { mainViewportWidth = it.width }
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AccountingBlurTopBar(backdrop = backdrop) {
                if (isWide) {
                    SmallTopAppBar(
                        title = activeTab.topBarTitle,
                        color = Color.Transparent,
                        scrollBehavior = scrollBehavior,
                        actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                        actions = {
                            if (activeTab == MainTab.HOME) {
                                TopBarIconAction(MiuixIcons.Add, "新增账户", onAddAccount)
                                Spacer(modifier = Modifier.width(TOP_BAR_ACTION_SPACING))
                            }
                            TopBarIconAction(MiuixIcons.GridView, "选择账本", onOpenLedgers)
                            Spacer(modifier = Modifier.width(TOP_BAR_ACTION_SPACING))
                            TopBarIconAction(MiuixIcons.Settings, "设置", onOpenSettings)
                        },
                    )
                } else {
                    TopAppBar(
                        title = activeTab.topBarTitle,
                        color = Color.Transparent,
                        scrollBehavior = scrollBehavior,
                        actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                        actions = {
                            if (activeTab == MainTab.HOME) {
                                TopBarIconAction(MiuixIcons.Add, "新增账户", onAddAccount)
                                Spacer(modifier = Modifier.width(TOP_BAR_ACTION_SPACING))
                            }
                            TopBarIconAction(MiuixIcons.GridView, "选择账本", onOpenLedgers)
                            Spacer(modifier = Modifier.width(TOP_BAR_ACTION_SPACING))
                            TopBarIconAction(MiuixIcons.Settings, "设置", onOpenSettings)
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
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = {
                                pagerCoroutineScope.launch {
                                    pagerState?.animateScrollToPage(
                                        page = tab.ordinal,
                                        animationSpec = tween(durationMillis = 300),
                                    )
                                }
                            },
                            icon = tab.icon,
                            label = tab.title,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val mobilePagerState = pagerState
            if ((isWide && selectedTab == MainTab.DETAILS) ||
                (mobilePagerState != null && mainViewportWidth > 0)
            ) {
                FloatingActionButton(
                    onClick = onOpenManualEntry,
                    modifier = Modifier
                        .offset(x = (-22.5f).dp, y = (-21.5f).dp)
                        .graphicsLayer {
                            if (mobilePagerState != null) {
                                translationX = mobilePagerState.getOffsetDistanceInPages(
                                    MainTab.DETAILS.ordinal,
                                ) * mainViewportWidth
                            }
                        },
                    containerColor = MiuixTheme.colorScheme.primaryContainer,
                    minWidth = 56.dp,
                    minHeight = 56.dp,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = "手动记账",
                        tint = Color.White,
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        val saveableStateHolder = rememberSaveableStateHolder()
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            if (pagerState == null) {
                saveableStateHolder.SaveableStateProvider(selectedTab) {
                    MainTabContent(
                        tab = selectedTab,
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onOpenAccount = onOpenAccount,
                        onOpenTransactionEdit = onOpenTransactionEdit,
                        onSelectLedger = onSelectLedger,
                    )
                }
            } else {
                MainTabPager(
                    pagerState = pagerState,
                    saveableStateHolder = saveableStateHolder,
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onOpenAccount = onOpenAccount,
                    onOpenTransactionEdit = onOpenTransactionEdit,
                    onSelectLedger = onSelectLedger,
                )
            }
        }
    }
}

/**
 * 在手机端承载三个一级页面并提供水平手势切换。
 */
@Composable
private fun MainTabPager(
    pagerState: PagerState,
    saveableStateHolder: SaveableStateHolder,
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
    onOpenAccount: (Long) -> Unit,
    onOpenTransactionEdit: (Long) -> Unit,
    onSelectLedger: (Long) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { MainTab.entries[it] },
    ) { page ->
        val tab = MainTab.entries[page]
        saveableStateHolder.SaveableStateProvider(tab) {
            MainTabContent(
                tab = tab,
                uiState = uiState,
                innerPadding = innerPadding,
                onOpenAccount = onOpenAccount,
                onOpenTransactionEdit = onOpenTransactionEdit,
                onSelectLedger = onSelectLedger,
            )
        }
    }
}

/**
 * 根据一级标签显示对应页面内容。
 */
@Composable
private fun MainTabContent(
    tab: MainTab,
    uiState: AccountingUiState,
    innerPadding: PaddingValues,
    onOpenAccount: (Long) -> Unit,
    onOpenTransactionEdit: (Long) -> Unit,
    onSelectLedger: (Long) -> Unit,
) {
    when (tab) {
        MainTab.HOME -> HomeScreen(
            uiState = uiState,
            innerPadding = innerPadding,
            onOpenAccount = onOpenAccount,
            onSelectLedger = onSelectLedger,
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
 * 为顶栏和底栏提供统一的 MIUIX Backdrop 模糊或不透明表面。
 */
@Composable
fun Modifier.accountingBarBlur(backdrop: LayerBackdrop): Modifier =
    if (isAccountingBlurSupported()) {
        textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 24f,
        ).background(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))
    } else {
        background(MiuixTheme.colorScheme.surface)
    }

/**
 * 判断当前系统是否能稳定绘制 MIUIX RuntimeShader 模糊。
 */
private fun isAccountingBlurSupported(): Boolean =
    isRuntimeShaderSupported() && Build.VERSION.SDK_INT != Build.VERSION_CODES.BAKLAVA
