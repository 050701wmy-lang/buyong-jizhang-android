package com.vos.accounting.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.AccountTypeEntity
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 展示全部预置和自定义账户类型，并从右上角创建新类型。
 */
@Composable
fun AccountTypeScreen(
    accountTypes: List<AccountTypeEntity>,
    selectedKey: String,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: (String, String, (String) -> Unit) -> Unit,
    onUpdate: (String, String, String, () -> Unit) -> Unit,
    onDelete: (String, () -> Unit) -> Unit,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingTypeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var managedTypeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var typeToDeleteKey by rememberSaveable { mutableStateOf<String?>(null) }
    val editingType = accountTypes.firstOrNull { it.key == editingTypeKey }
    val managedType = accountTypes.firstOrNull { it.key == managedTypeKey }
    val typeToDelete = accountTypes.firstOrNull { it.key == typeToDeleteKey }
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AccountingBlurTopBar(backdrop = backdrop) {
                TopAppBar(
                    title = "选择账户类型",
                    color = Color.Transparent,
                    navigationIcon = {
                        IconButton(onClick = onBack, minWidth = 35.dp, minHeight = 35.dp) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    },
                    actions = {
                        TopBarIconAction(MiuixIcons.Add, "自定义账户类型") {
                            editingTypeKey = null
                            showEditor = true
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                )
            }
        },
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
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    AccountSectionTitle("预置类型")
                }
                item {
                    AccountTypeCard(
                        accountTypes = accountTypes.filter(AccountTypeEntity::isBuiltin),
                        selectedKey = selectedKey,
                        onSelect = onSelect,
                        onLongClick = { managedTypeKey = it.key },
                    )
                }
                item {
                    AccountSectionTitle("自定义类型")
                }
                if (accountTypes.any { !it.isBuiltin }) {
                    item {
                        AccountTypeCard(
                            accountTypes = accountTypes.filterNot(AccountTypeEntity::isBuiltin),
                            selectedKey = selectedKey,
                            onSelect = onSelect,
                            onLongClick = { managedTypeKey = it.key },
                        )
                    }
                } else {
                    item {
                        EmptyCard(text = "暂无自定义类型")
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(24.dp)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }
    AccountTypeEditorDialog(
        show = showEditor,
        accountType = editingType,
        writeInProgress = writeInProgress,
        onDismiss = {
            showEditor = false
            editingTypeKey = null
        },
        onConfirm = { name, summary ->
            if (editingType == null) {
                onAdd(name, summary) { typeKey ->
                    showEditor = false
                    onSelect(typeKey)
                }
            } else {
                onUpdate(editingType.key, name, summary) {
                    showEditor = false
                    editingTypeKey = null
                }
            }
        },
    )
    AccountTypeManageDialog(
        accountType = managedType,
        onDismiss = { managedTypeKey = null },
        onEdit = { accountType ->
            managedTypeKey = null
            editingTypeKey = accountType.key
            showEditor = true
        },
        onDelete = { accountType ->
            managedTypeKey = null
            typeToDeleteKey = accountType.key
        },
    )
    AccountTypeDeleteDialog(
        accountType = typeToDelete,
        writeInProgress = writeInProgress,
        onDismiss = { typeToDeleteKey = null },
        onConfirm = { accountType ->
            onDelete(accountType.key) {
                typeToDeleteKey = null
                if (accountType.key == selectedKey) {
                    onSelect("cash")
                }
            }
        },
    )
}

/**
 * 长按自定义账户类型后提供编辑和删除操作。
 */
@Composable
private fun AccountTypeManageDialog(
    accountType: AccountTypeEntity?,
    onDismiss: () -> Unit,
    onEdit: (AccountTypeEntity) -> Unit,
    onDelete: (AccountTypeEntity) -> Unit,
) {
    WindowDialog(
        show = accountType != null,
        title = accountType?.name ?: "管理账户类型",
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { accountType?.let(onEdit) },
                modifier = Modifier.weight(1f),
                enabled = accountType != null,
            ) {
                Text(text = "编辑")
            }
            Button(
                onClick = { accountType?.let(onDelete) },
                modifier = Modifier.weight(1f),
                enabled = accountType != null,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = "删除")
            }
        }
    }
}

/**
 * 在一个连续 Card 中展示全部账户类型选项。
 */
@Composable
private fun AccountTypeCard(
    accountTypes: List<AccountTypeEntity>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onLongClick: (AccountTypeEntity) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        accountTypes.forEach { accountType ->
            AccountTypeRow(
                accountType = accountType,
                selected = accountType.key == selectedKey,
                onClick = { onSelect(accountType.key) },
                onLongClick = { onLongClick(accountType) },
            )
        }
    }
}

/**
 * 展示一个带图标、名称、可选说明和选中标记的账户类型。
 */
@Composable
private fun AccountTypeRow(
    accountType: AccountTypeEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    BasicComponent(
        title = accountType.name,
        summary = accountType.summary.ifEmpty { null },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (accountType.isBuiltin) null else onLongClick,
            ),
        titleColor = if (selected) {
            BasicComponentDefaults.titleColor(
                color = MiuixTheme.colorScheme.primary,
            )
        } else {
            BasicComponentDefaults.titleColor()
        },
        startAction = {
            AccountIcon(
                iconKey = accountType.iconKey,
                modifier = Modifier.size(36.dp),
            )
        },
        endActions = if (selected) {
            {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = "已选择",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
    )
}

/**
 * 确认删除一个未被账户引用的自定义账户类型。
 */
@Composable
private fun AccountTypeDeleteDialog(
    accountType: AccountTypeEntity?,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AccountTypeEntity) -> Unit,
) {
    WindowDialog(
        show = accountType != null,
        title = "删除账户类型",
        summary = accountType?.let { "确定删除“${it.name}”吗？" },
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = { accountType?.let(onConfirm) },
                modifier = Modifier.weight(1f),
                enabled = accountType != null && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "删除中…" else "删除")
            }
        }
    }
}

/**
 * 输入自定义账户类型名称与可选说明，并在确认后交给统一写入状态处理。
 */
@Composable
private fun AccountTypeEditorDialog(
    show: Boolean,
    accountType: AccountTypeEntity?,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var summary by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    LaunchedEffect(show, accountType?.key) {
        if (show) {
            name = TextFieldValue(accountType?.name.orEmpty())
            summary = TextFieldValue(accountType?.summary.orEmpty())
        }
    }
    WindowDialog(
        show = show,
        title = if (accountType == null) "自定义账户类型" else "编辑账户类型",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = "类型名称",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        TextField(
            value = summary,
            onValueChange = { summary = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = "类型说明（选填）",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = { onConfirm(name.text, summary.text) },
                modifier = Modifier.weight(1f),
                enabled = name.text.isNotBlank() && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "保存中…" else "确定")
            }
        }
    }
}
