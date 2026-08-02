package com.vos.accounting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 展示手动记账表单并在确认后提交正式账目。
 */
@Composable
fun ManualEntryScreen(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    initialAccountId: Long = 0,
    transaction: TransactionRecord? = null,
    onSave: (TransactionDraft, () -> Unit) -> Unit,
    onUpdate: (Long, TransactionDraft, () -> Unit) -> Unit = { _, _, _ -> },
    onDelete: (Long, () -> Unit) -> Unit = { _, _ -> },
    onAddCategory: (
        String,
        TransactionType,
        String,
        (Long) -> Unit,
        () -> Unit,
    ) -> Unit,
) {
    var amountExpression by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.amountMinor?.let(::manualFixedAmountText).orEmpty())
    }
    var note by rememberSaveable(transaction?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(transaction?.note.orEmpty()))
    }
    var merchant by rememberSaveable(transaction?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(transaction?.merchant.orEmpty()))
    }
    var type by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.type ?: TransactionType.EXPENSE)
    }
    var accountId by rememberSaveable(transaction?.id, initialAccountId) {
        mutableStateOf(transaction?.accountId ?: initialAccountId)
    }
    var categoryId by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.categoryId ?: 0L)
    }
    var keypadVisible by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction == null)
    }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var occurredAt by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.occurredAt ?: System.currentTimeMillis())
    }
    val matchingCategories = uiState.categories.filter {
        it.type == type && (!it.isArchived || it.id == transaction?.categoryId)
    }
    val selectableAccounts = uiState.accounts.filter {
        !it.isArchived || it.id == transaction?.accountId
    }
    val amountMinor = calculateManualAmount(amountExpression)

    LaunchedEffect(selectableAccounts) {
        if (selectableAccounts.none { it.id == accountId }) {
            accountId = selectableAccounts.firstOrNull(AccountEntity::isDefault)?.id
                ?: selectableAccounts.firstOrNull()?.id
                ?: 0
        }
    }
    LaunchedEffect(type, matchingCategories) {
        if (matchingCategories.none { it.id == categoryId }) {
            categoryId = matchingCategories.firstOrNull()?.id ?: 0
        }
    }

    SecondaryScaffold(
        title = if (transaction == null) "记一笔" else "编辑",
        backdrop = backdrop,
        onBack = onBack,
        navigationIcon = MiuixIcons.Close,
        navigationContentDescription = "关闭",
    ) { innerPadding ->
        ManualEntryContent(
            innerPadding = innerPadding,
            amountExpression = amountExpression,
            amountMinor = amountMinor,
            type = type,
            accounts = selectableAccounts,
            accountId = accountId,
            categories = matchingCategories,
            categoryId = categoryId,
            note = note,
            merchant = merchant,
            occurredAt = occurredAt,
            canSave = amountMinor != null &&
                accountId != 0L &&
                matchingCategories.any { it.id == categoryId } &&
                !uiState.writeInProgress,
            writeInProgress = uiState.writeInProgress,
            editMode = transaction != null,
            keypadVisible = keypadVisible,
            onSelectType = { type = it },
            onSelectAccount = { accountId = it },
            onSelectCategory = { categoryId = it },
            onAddCategory = { showCategoryDialog = true },
            onNoteChange = { note = it },
            onMerchantChange = { merchant = it },
            onOccurredAtChange = { occurredAt = it },
            onShowKeypad = { keypadVisible = true },
            onHideKeypad = { keypadVisible = false },
            onAmountKey = { amountExpression = appendManualAmountKey(amountExpression, it) },
            onAmountDelete = { amountExpression = amountExpression.dropLast(1) },
            onDelete = { showDeleteDialog = true },
            onSave = {
                amountMinor?.let {
                    val draft = TransactionDraft(
                        type = type,
                        amountMinor = it,
                        accountId = accountId,
                        categoryId = categoryId,
                        merchant = merchant.text,
                        note = note.text,
                        occurredAt = occurredAt,
                        source = transaction?.source ?: TransactionSource.MANUAL,
                    )
                    if (transaction == null) {
                        onSave(draft, onBack)
                    } else {
                        onUpdate(transaction.id, draft, onBack)
                    }
                }
            },
        )
    }
    CategoryEditorDialog(
        show = showCategoryDialog,
        type = type,
        onDismiss = { showCategoryDialog = false },
        onAdd = { name, iconKey, onAdded, onDuplicate ->
            onAddCategory(name, type, iconKey, onAdded, onDuplicate)
        },
        onCreated = { newCategoryId ->
            categoryId = newCategoryId
            showCategoryDialog = false
        },
        writeInProgress = uiState.writeInProgress,
    )
    DeleteTransactionDialog(
        show = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            transaction?.let { onDelete(it.id, onBack) }
        },
        writeInProgress = uiState.writeInProgress,
    )
}

/**
 * 组织手动记账页的金额、分类、附加信息和固定键盘。
 */
@Composable
private fun ManualEntryContent(
    innerPadding: PaddingValues,
    amountExpression: String,
    amountMinor: Long?,
    type: TransactionType,
    accounts: List<AccountEntity>,
    accountId: Long,
    categories: List<CategoryEntity>,
    categoryId: Long,
    note: TextFieldValue,
    merchant: TextFieldValue,
    occurredAt: Long,
    canSave: Boolean,
    writeInProgress: Boolean,
    editMode: Boolean,
    keypadVisible: Boolean,
    onSelectType: (TransactionType) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectCategory: (Long) -> Unit,
    onAddCategory: () -> Unit,
    onNoteChange: (TextFieldValue) -> Unit,
    onMerchantChange: (TextFieldValue) -> Unit,
    onOccurredAtChange: (Long) -> Unit,
    onShowKeypad: () -> Unit,
    onHideKeypad: () -> Unit,
    onAmountKey: (String) -> Unit,
    onAmountDelete: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    var noteFocused by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 800.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            ManualTypeTabs(
                type = type,
                onSelectType = onSelectType,
            )
            ManualAmountDisplay(
                amountExpression = amountExpression,
                invalid = amountExpression.isNotBlank() && amountMinor == null,
                onClick = onShowKeypad,
            )
            ManualCategoryGrid(
                categories = categories,
                selectedId = categoryId,
                onSelect = onSelectCategory,
                onAdd = onAddCategory,
            )
            ManualDetailRows(
                accounts = accounts,
                accountId = accountId,
                occurredAt = occurredAt,
                merchant = merchant,
                note = note,
                onSelectAccount = onSelectAccount,
                onOccurredAtChange = onOccurredAtChange,
                onMerchantChange = onMerchantChange,
                onNoteChange = onNoteChange,
                onNoteFocusChange = { noteFocused = it },
            )
        }
        if (!noteFocused && keypadVisible) {
            ManualKeypad(
                onAmountKey = onAmountKey,
                onDelete = onAmountDelete,
                onSave = onSave,
                saveEnabled = canSave,
                writeInProgress = writeInProgress,
            )
        } else if (!noteFocused && editMode) {
            ManualEditActions(
                saveEnabled = canSave,
                writeInProgress = writeInProgress,
                onDelete = onDelete,
                onSave = onSave,
            )
        }
    }
    LaunchedEffect(noteFocused) {
        if (noteFocused) onHideKeypad()
    }
}

/**
 * 展示支出与收入类型标签。
 */
@Composable
private fun ManualTypeTabs(
    type: TransactionType,
    onSelectType: (TransactionType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ManualTypeTab(
            title = "支出",
            selected = type == TransactionType.EXPENSE,
            onClick = { onSelectType(TransactionType.EXPENSE) },
        )
        ManualTypeTab(
            title = "收入",
            selected = type == TransactionType.INCOME,
            onClick = { onSelectType(TransactionType.INCOME) },
        )
    }
}

/**
 * 展示带底部主色指示线的单个类型标签。
 */
@Composable
private fun ManualTypeTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = if (selected) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MiuixTheme.textStyles.title4,
        )
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .width(24.dp)
                .height(3.dp)
                .squircleBackground(
                    color = if (selected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    cornerRadius = 2.dp,
                ),
        )
    }
}

/**
 * 展示人民币符号、金额表达式和输入校验状态。
 */
@Composable
private fun ManualAmountDisplay(
    amountExpression: String,
    invalid: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¥",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            Text(
                text = amountExpression.ifBlank { "0.00" },
                modifier = Modifier.padding(start = 8.dp),
                color = if (amountExpression.isBlank()) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.35f)
                } else {
                    MiuixTheme.colorScheme.onBackground
                },
                fontWeight = FontWeight.Medium,
                fontSize = 40.sp,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MiuixTheme.colorScheme.primary),
        )
        if (invalid) {
            Text(
                text = "请输入大于 0 且最多两位小数的金额",
                modifier = Modifier.padding(top = 6.dp),
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
    }
}

/**
 * 按每行五项展示当前收支类型的真实分类。
 */
@Composable
private fun ManualCategoryGrid(
    categories: List<CategoryEntity>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val slotCount = categories.size + 1
        repeat((slotCount + 4) / 5) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(5) { columnIndex ->
                    val itemIndex = rowIndex * 5 + columnIndex
                    when {
                        itemIndex < categories.size -> {
                            val category = categories[itemIndex]
                            ManualCategoryItem(
                                modifier = Modifier.weight(1f),
                                category = category,
                                selected = selectedId == category.id,
                                onClick = { onSelect(category.id) },
                            )
                        }

                        itemIndex == categories.size -> ManualCategoryAddItem(
                            modifier = Modifier.weight(1f),
                            onClick = onAdd,
                        )

                        else -> Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 展示一个带 MIUIX 图标和 squircle 选中态的分类。
 */
@Composable
private fun ManualCategoryItem(
    modifier: Modifier,
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .squircleBackground(
                    color = if (selected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    cornerRadius = 15.dp,
                )
                .padding(if (selected) 0.dp else 1.dp)
                .squircleBackground(
                    color = if (selected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.surface
                    },
                    cornerRadius = 14.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = accountingCategoryIcon(
                    iconKey = category.iconKey,
                    fallbackName = category.name,
                ),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = if (selected) {
                    Color.White
                } else {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.72f)
                },
            )
        }
        Text(
            text = category.name,
            modifier = Modifier.padding(top = 7.dp),
            color = if (selected) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

/**
 * 展示分类宫格末尾的添加入口。
 */
@Composable
private fun ManualCategoryAddItem(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    cornerRadius = 28.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Add,
                contentDescription = "添加分类",
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = "添加",
            modifier = Modifier.padding(top = 7.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

/**
 * 展示记账时间和可编辑备注。
 */
@Composable
private fun ManualDetailRows(
    accounts: List<AccountEntity>,
    accountId: Long,
    occurredAt: Long,
    merchant: TextFieldValue,
    note: TextFieldValue,
    onSelectAccount: (Long) -> Unit,
    onOccurredAtChange: (Long) -> Unit,
    onMerchantChange: (TextFieldValue) -> Unit,
    onNoteChange: (TextFieldValue) -> Unit,
    onNoteFocusChange: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showAccountPicker by rememberSaveable { mutableStateOf(false) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable { showDateTimePicker = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Timer,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "时间",
                modifier = Modifier.padding(start = 8.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            Text(
                text = formatManualEntryTime(occurredAt),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                textAlign = TextAlign.Start,
                style = MiuixTheme.textStyles.body2,
            )
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        HorizontalDivider()
        ManualTextInputRow(
            icon = MiuixIcons.Store,
            title = "对象",
            value = merchant,
            placeholder = "点击添加商家或对象",
            focusManager = focusManager,
            onValueChange = onMerchantChange,
            onFocusChange = onNoteFocusChange,
        )
        HorizontalDivider()
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clickable { showAccountPicker = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.BankCards,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = "账户",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = accounts.firstOrNull { it.id == accountId }?.name.orEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowUpDown,
                    contentDescription = null,
                    modifier = Modifier.size(width = 10.dp, height = 16.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            AccountPickerPopup(
                show = showAccountPicker,
                accounts = accounts,
                selectedId = accountId,
                onDismiss = { showAccountPicker = false },
                onSelect = {
                    onSelectAccount(it)
                    showAccountPicker = false
                },
            )
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Notes,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "备注",
                modifier = Modifier.padding(start = 8.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp)
                    .onFocusChanged { onNoteFocusChange(it.isFocused) },
                textStyle = MiuixTheme.textStyles.body2.copy(
                    color = MiuixTheme.colorScheme.onBackground,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (note.text.isEmpty()) {
                            Text(
                                text = "点击添加备注",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(
                                    alpha = 0.45f,
                                ),
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        HorizontalDivider()
    }
    ManualDateTimeDialog(
        show = showDateTimePicker,
        occurredAt = occurredAt,
        onDismiss = { showDateTimePicker = false },
        onConfirm = {
            onOccurredAtChange(it)
            showDateTimePicker = false
        },
    )
}

/**
 * 展示记账附加信息中的单行文本输入。
 */
@Composable
private fun ManualTextInputRow(
    icon: ImageVector,
    title: String,
    value: TextFieldValue,
    placeholder: String,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)
                .onFocusChanged { onFocusChange(it.isFocused) },
            textStyle = MiuixTheme.textStyles.body2.copy(
                color = MiuixTheme.colorScheme.onBackground,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.45f),
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

/**
 * 使用 MIUIX 数字选择器编辑账目的本地日期与时间。
 */
@Composable
private fun ManualDateTimeDialog(
    show: Boolean,
    occurredAt: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zoneId = ZoneId.systemDefault()
    val initialDateTime = Instant.ofEpochMilli(occurredAt).atZone(zoneId).toLocalDateTime()
    var year by rememberSaveable(show) { mutableStateOf(initialDateTime.year) }
    var month by rememberSaveable(show) { mutableStateOf(initialDateTime.monthValue) }
    var day by rememberSaveable(show) { mutableStateOf(initialDateTime.dayOfMonth) }
    var hour by rememberSaveable(show) { mutableStateOf(initialDateTime.hour) }
    var minute by rememberSaveable(show) { mutableStateOf(initialDateTime.minute) }
    val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
    LaunchedEffect(maxDay) {
        day = day.coerceAtMost(maxDay)
    }
    WindowDialog(
        show = show,
        title = "修改时间",
        onDismissRequest = onDismiss,
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                NumberPicker(
                    value = year,
                    onValueChange = { year = it },
                    modifier = Modifier.weight(1.2f),
                    range = 1970..2100,
                    label = { "${it}年" },
                    visibleItemCount = 3,
                )
                NumberPicker(
                    value = month,
                    onValueChange = { month = it },
                    modifier = Modifier.weight(1f),
                    range = 1..12,
                    label = { "${it}月" },
                    visibleItemCount = 3,
                    wrapAround = true,
                )
                NumberPicker(
                    value = day,
                    onValueChange = { day = it },
                    modifier = Modifier.weight(1f),
                    range = 1..maxDay,
                    label = { "${it}日" },
                    visibleItemCount = 3,
                    wrapAround = true,
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                NumberPicker(
                    value = hour,
                    onValueChange = { hour = it },
                    modifier = Modifier.weight(1f),
                    range = 0..23,
                    label = { "%02d时".format(it) },
                    visibleItemCount = 3,
                    wrapAround = true,
                )
                NumberPicker(
                    value = minute,
                    onValueChange = { minute = it },
                    modifier = Modifier.weight(1f),
                    range = 0..59,
                    label = { "%02d分".format(it) },
                    visibleItemCount = 3,
                    wrapAround = true,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "取消")
                }
                Button(
                    onClick = {
                        onConfirm(
                            LocalDateTime.of(year, month, day, hour, minute)
                                .atZone(zoneId)
                                .toInstant()
                                .toEpochMilli(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(text = "确认")
                }
            }
        }
    }
}

/**
 * 展示编辑账目时固定在底部的删除与保存操作。
 */
@Composable
private fun ManualEditActions(
    saveEnabled: Boolean,
    writeInProgress: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            enabled = !writeInProgress,
        ) {
            Text(text = "删除")
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = saveEnabled,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(text = if (writeInProgress) "保存中…" else "保存")
        }
    }
}

/**
 * 以 HyperOS 风格弹出列表展示可用账户。
 */
@Composable
private fun AccountPickerPopup(
    show: Boolean,
    accounts: List<AccountEntity>,
    selectedId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    WindowListPopup(
        show = show,
        popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(
            horizontalMargin = 12.dp,
        ),
        alignment = PopupPositionProvider.Align.End,
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        maxHeight = 440.dp,
        minWidth = 288.dp,
    ) {
        ListPopupColumn {
            accounts.forEach { account ->
                PopupSelectionRow(
                    title = account.name,
                    summary = accountTypeTitle(account.type),
                    selected = account.id == selectedId,
                    onClick = { onSelect(account.id) },
                )
            }
        }
    }
}

/**
 * 展示新增分类的名称与图标选择 Window Dialog。
 */
@Composable
private fun CategoryEditorDialog(
    show: Boolean,
    type: TransactionType,
    onDismiss: () -> Unit,
    onAdd: (String, String, (Long) -> Unit, () -> Unit) -> Unit,
    onCreated: (Long) -> Unit,
    writeInProgress: Boolean,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var iconKey by rememberSaveable { mutableStateOf(categoryIconOptions.first().key) }
    var duplicate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(show, type) {
        if (show) {
            name = TextFieldValue()
            iconKey = categoryIconOptions.first().key
            duplicate = false
        }
    }
    WindowDialog(
        show = show,
        title = "添加分类",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = name,
            onValueChange = {
                name = it
                duplicate = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = if (duplicate) "不能与已有分类名称重复" else "分类名称",
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categoryIconOptions.chunked(5).forEach { rowIcons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    rowIcons.forEach { option ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .squircleBackground(
                                    color = if (option.key == iconKey) {
                                        MiuixTheme.colorScheme.primary
                                    } else {
                                        MiuixTheme.colorScheme.secondaryContainer
                                    },
                                    cornerRadius = 14.dp,
                                )
                                .squircleClip(14.dp)
                                .clickable { iconKey = option.key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (option.key == iconKey) {
                                    Color.White
                                } else {
                                    MiuixTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                    repeat(5 - rowIcons.size) {
                        Spacer(modifier = Modifier.size(50.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = {
                    onAdd(
                        name.text,
                        iconKey,
                        onCreated,
                        { duplicate = true },
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = name.text.isNotBlank() && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "保存中…" else "确定")
            }
        }
    }
}

/**
 * 展示删除账目的二次确认 Window Dialog。
 */
@Composable
private fun DeleteTransactionDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    writeInProgress: Boolean,
) {
    WindowDialog(
        show = show,
        title = "删除账目",
        summary = "删除后无法恢复，是否继续？",
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
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "删除中…" else "确认")
            }
        }
    }
}

/**
 * 展示由统一写入流程返回的可恢复错误。
 */
@Composable
fun AccountingWriteErrorDialog(
    message: String?,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = message != null,
        title = "操作未完成",
        summary = message,
        onDismissRequest = onDismiss,
    ) {
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(text = "知道了")
        }
    }
}

/**
 * 展示新增或编辑账户的独立二级页面。
 */
@Composable
fun AccountEditorScreen(
    account: AccountEntity?,
    currentBalanceMinor: Long,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSave: (AccountEntity, () -> Unit) -> Unit,
    onArchive: (Long, () -> Unit) -> Unit,
) {
    var name by rememberSaveable(account?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(account?.name.orEmpty()))
    }
    var balance by rememberSaveable(account?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                if (account == null) "" else manualAmountText(currentBalanceMinor),
            ),
        )
    }
    var type by rememberSaveable(account?.id) {
        mutableStateOf(account?.type ?: AccountType.CASH)
    }
    var isDefault by rememberSaveable(account?.id) {
        mutableStateOf(account?.isDefault ?: false)
    }
    var isArchived by rememberSaveable(account?.id) {
        mutableStateOf(account?.isArchived ?: false)
    }
    var showTypePopup by rememberSaveable { mutableStateOf(false) }
    var showBalanceDialog by rememberSaveable { mutableStateOf(false) }
    val balanceMinor = parseSignedMoneyToMinor(balance.text)
    val transactionNetMinor = if (account == null) {
        0
    } else {
        currentBalanceMinor - account.openingBalanceMinor
    }
    val canSave = name.text.isNotBlank() && balanceMinor != null && !writeInProgress
    val scrollBehavior = MiuixScrollBehavior()
    val saveAccount = {
        onSave(
            (account ?: AccountEntity(
                name = "",
                type = type,
                openingBalanceMinor = 0,
                sortOrder = 0,
            )).copy(
                name = name.text,
                type = type,
                openingBalanceMinor = (balanceMinor ?: 0) - transactionNetMinor,
                isDefault = isDefault,
                isArchived = isArchived,
            ),
            onBack,
        )
    }
    val pageTitle = if (account == null) "添加账户" else "编辑账户"
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AccountEditorTopBar(
                    title = pageTitle,
                    isWide = isWide,
                    canSave = canSave,
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    onBack = onBack,
                    onSave = saveAccount,
                )
            },
            bottomBar = {
                AccountEditorActions(
                    account = account,
                    canSave = canSave,
                    writeInProgress = writeInProgress,
                    backdrop = backdrop,
                    onArchive = { account?.let { onArchive(it.id, onBack) } },
                    onSave = saveAccount,
                )
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
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            label = "账户名称",
                            useLabelAsPlaceholder = false,
                            singleLine = true,
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                BasicComponent(
                                    title = "账户类型",
                                    modifier = Modifier.fillMaxWidth(),
                                    endActions = {
                                        Text(
                                            text = accountTypeTitle(type),
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            style = MiuixTheme.textStyles.body2,
                                        )
                                        Icon(
                                            imageVector = MiuixIcons.Basic.ArrowUpDown,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(start = 6.dp)
                                                .size(width = 10.dp, height = 16.dp),
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        )
                                    },
                                    onClick = { showTypePopup = true },
                                )
                                AccountTypePopup(
                                    show = showTypePopup,
                                    selectedType = type,
                                    onDismiss = { showTypePopup = false },
                                    onSelect = {
                                        type = it
                                        showTypePopup = false
                                    },
                                )
                            }
                            BasicComponent(
                                title = "账户图标",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Icon(
                                        imageVector = accountTypeIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MiuixTheme.colorScheme.primary,
                                    )
                                },
                            )
                            BasicComponent(
                                title = "账户币种",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Text(
                                        text = "人民币",
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        style = MiuixTheme.textStyles.body2,
                                    )
                                },
                            )
                            ArrowPreference(
                                title = "账户余额",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Text(
                                        text = balanceMinor?.let(::formatDecimalAmount).orEmpty(),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        style = MiuixTheme.textStyles.body2,
                                    )
                                },
                                onClick = { showBalanceDialog = true },
                            )
                        }
                    }
                    item {
                        AccountEditorSectionTitle(text = "账户设置")
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            BasicComponent(
                                title = "适用账本",
                                summary = "设置此账户可在哪些账本中使用",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Text(
                                        text = "全部",
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        style = MiuixTheme.textStyles.body2,
                                    )
                                },
                            )
                            SwitchPreference(
                                checked = isDefault,
                                onCheckedChange = { isDefault = it },
                                title = "默认账户",
                                summary = "记一笔时优先选择此账户",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SwitchPreference(
                                checked = isArchived,
                                onCheckedChange = { isArchived = it },
                                title = "隐藏账户",
                                summary = "首页不再显示，历史账目仍然保留",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item {
                        Spacer(
                            modifier = Modifier.height(
                                innerPadding.calculateBottomPadding() + 24.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
    AccountBalanceDialog(
        show = showBalanceDialog,
        balance = balance,
        onBalanceChange = { balance = it },
        onDismiss = { showBalanceDialog = false },
        onConfirm = { showBalanceDialog = false },
    )
}

/**
 * 根据窗口宽度展示可折叠大标题或固定小标题账户编辑顶栏。
 */
@Composable
private fun AccountEditorTopBar(
    title: String,
    isWide: Boolean,
    canSave: Boolean,
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSave: () -> Unit,
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
            onClick = onSave,
            enabled = canSave,
            backgroundColor = Color.Transparent,
            minWidth = TOP_BAR_ACTION_BUTTON_SIZE,
            minHeight = TOP_BAR_ACTION_BUTTON_SIZE,
        ) {
            Icon(
                imageVector = MiuixIcons.Ok,
                contentDescription = "保存账户",
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
 * 展示账户编辑页的蓝色分区标题。
 */
@Composable
private fun AccountEditorSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        color = MiuixTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        style = MiuixTheme.textStyles.body2,
    )
}

/**
 * 展示账户编辑页固定在底部的停用与保存操作。
 */
@Composable
private fun AccountEditorActions(
    account: AccountEntity?,
    canSave: Boolean,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onArchive: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .accountingBarBlur(backdrop)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (account != null) {
            Button(
                onClick = onArchive,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = if (writeInProgress) "处理中…" else "停用")
            }
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = canSave,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(text = if (writeInProgress) "保存中…" else "保存")
        }
    }
}

/**
 * 以 HyperOS 风格弹出列表展示固定账户类型选项。
 */
@Composable
private fun AccountTypePopup(
    show: Boolean,
    selectedType: AccountType,
    onDismiss: () -> Unit,
    onSelect: (AccountType) -> Unit,
) {
    WindowListPopup(
        show = show,
        popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(
            horizontalMargin = 12.dp,
        ),
        alignment = PopupPositionProvider.Align.End,
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        maxHeight = 520.dp,
        minWidth = 288.dp,
    ) {
        ListPopupColumn {
            AccountType.entries.forEach { type ->
                PopupSelectionRow(
                    title = accountTypeTitle(type),
                    summary = accountTypeSummary(type),
                    selected = type == selectedType,
                    onClick = { onSelect(type) },
                )
            }
        }
    }
}

/**
 * 展示可复用的 HyperOS 弹出单选项。
 */
@Composable
private fun PopupSelectionRow(
    title: String,
    summary: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(288.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.Medium,
                style = MiuixTheme.textStyles.body1,
            )
            summary?.let {
                Text(
                    text = it,
                    color = if (selected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = MiuixIcons.Ok,
                contentDescription = "当前选项",
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * 展示账户余额输入对话框并仅在确认后保留输入状态。
 */
@Composable
private fun AccountBalanceDialog(
    show: Boolean,
    balance: TextFieldValue,
    onBalanceChange: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var editingBalance by rememberSaveable(show, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(balance)
    }
    val parsedBalance = parseSignedMoneyToMinor(editingBalance.text)
    LaunchedEffect(show) {
        if (show) editingBalance = balance
    }
    WindowDialog(
        show = show,
        title = "修改账户余额",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = editingBalance,
            onValueChange = { editingBalance = it },
            modifier = Modifier.fillMaxWidth(),
            label = "账户余额",
            useLabelAsPlaceholder = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = {
                    onBalanceChange(editingBalance)
                    onConfirm()
                },
                modifier = Modifier.weight(1f),
                enabled = parsedBalance != null,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = "确认")
            }
        }
    }
}

/**
 * 返回账户类型对应的主题图标。
 */
internal fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH,
    AccountType.BANK_CARD,
    AccountType.CREDIT,
    -> MiuixIcons.BankCards
    AccountType.ONLINE -> MiuixIcons.CloudFill
    AccountType.INVESTMENT -> MiuixIcons.Promotions
    AccountType.STORED_VALUE -> MiuixIcons.Store
    AccountType.VIRTUAL -> MiuixIcons.Layers
}

/**
 * 返回账户类型的中文标题。
 */
internal fun accountTypeTitle(type: AccountType): String = when (type) {
    AccountType.CASH -> "现金"
    AccountType.BANK_CARD -> "储蓄卡"
    AccountType.CREDIT -> "信用账户"
    AccountType.ONLINE -> "网络账户"
    AccountType.INVESTMENT -> "投资账户"
    AccountType.STORED_VALUE -> "储值卡"
    AccountType.VIRTUAL -> "虚拟账户"
}

/**
 * 返回账户类型在选择页使用的说明文字。
 */
internal fun accountTypeSummary(type: AccountType): String = when (type) {
    AccountType.CASH -> "持有的现金资产"
    AccountType.BANK_CARD -> "银行储蓄卡、借记卡"
    AccountType.CREDIT -> "银行信用卡、花呗、美团月付等"
    AccountType.ONLINE -> "支付宝、微信钱包、QQ 钱包等"
    AccountType.INVESTMENT -> "理财、基金、股票等"
    AccountType.STORED_VALUE -> "购物卡、饭卡、加油卡等"
    AccountType.VIRTUAL -> "积分、游戏币等虚拟资产"
}

/**
 * 将允许正负数和零的账户余额输入转换为最小货币单位。
 */
private fun parseSignedMoneyToMinor(text: String): Long? {
    if (text.isBlank()) return 0
    val amount = text.trim().toBigDecimalOrNull() ?: return null
    if (amount.scale() > 2) return null
    return try {
        amount.movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    }
}

/**
 * 展示固定在页面底部的四列数字键盘。
 */
@Composable
private fun ManualKeypad(
    onAmountKey: (String) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    writeInProgress: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(6.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        ManualKeypadRow {
            ManualKeypadButton("1", Modifier.weight(1f)) { onAmountKey("1") }
            ManualKeypadButton("2", Modifier.weight(1f)) { onAmountKey("2") }
            ManualKeypadButton("3", Modifier.weight(1f)) { onAmountKey("3") }
            ManualKeypadDeleteButton(Modifier.weight(1f), onDelete)
        }
        ManualKeypadRow {
            ManualKeypadButton("4", Modifier.weight(1f)) { onAmountKey("4") }
            ManualKeypadButton("5", Modifier.weight(1f)) { onAmountKey("5") }
            ManualKeypadButton("6", Modifier.weight(1f)) { onAmountKey("6") }
            ManualKeypadButton(
                text = "+",
                modifier = Modifier.weight(1f),
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
            ) {
                onAmountKey("+")
            }
        }
        ManualKeypadRow {
            ManualKeypadButton("7", Modifier.weight(1f)) { onAmountKey("7") }
            ManualKeypadButton("8", Modifier.weight(1f)) { onAmountKey("8") }
            ManualKeypadButton("9", Modifier.weight(1f)) { onAmountKey("9") }
            ManualKeypadButton(
                text = "−",
                modifier = Modifier.weight(1f),
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
            ) {
                onAmountKey("−")
            }
        }
        ManualKeypadRow {
            ManualKeypadButton(".", Modifier.weight(1f)) { onAmountKey(".") }
            ManualKeypadButton("0", Modifier.weight(1f)) { onAmountKey("0") }
            ManualKeypadButton(
                text = "再记一笔",
                modifier = Modifier.weight(1f),
                enabled = false,
                textSize = 13.sp,
            ) {}
            ManualKeypadButton(
                text = if (writeInProgress) "保存中" else "完成",
                modifier = Modifier.weight(1f),
                backgroundColor = MiuixTheme.colorScheme.primary,
                contentColor = Color.White,
                enabled = saveEnabled,
                textSize = 17.sp,
                onClick = onSave,
            )
        }
    }
}

/**
 * 为数字键盘的一行提供统一高度。
 */
@Composable
private fun ManualKeypadRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

/**
 * 展示数字键盘的文字按键。
 */
@Composable
private fun ManualKeypadButton(
    text: String,
    modifier: Modifier,
    backgroundColor: Color = MiuixTheme.colorScheme.surface,
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    textSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .height(54.dp)
            .squircleBackground(
                color = if (enabled) {
                    backgroundColor
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
                cornerRadius = 13.dp,
            )
            .squircleClip(13.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) {
                contentColor
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.45f)
            },
            fontWeight = if (text == "完成") FontWeight.Bold else FontWeight.Normal,
            fontSize = textSize,
            maxLines = 1,
        )
    }
}

/**
 * 展示数字键盘的删除按键。
 */
@Composable
private fun ManualKeypadDeleteButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .height(54.dp)
            .squircleBackground(
                color = MiuixTheme.colorScheme.secondaryContainer,
                cornerRadius = 13.dp,
            )
            .squircleClip(13.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "⌫",
            fontSize = 24.sp,
        )
    }
}

/**
 * 在金额表达式末尾追加一个经过格式约束的键盘输入。
 */
internal fun appendManualAmountKey(
    expression: String,
    key: String,
): String {
    if (key == "+" || key == "−") {
        if (expression.isBlank()) return expression
        if (expression.last() == '+' || expression.last() == '−') {
            return expression.dropLast(1) + key
        }
        val normalized = if (expression.drop(1).any { it == '+' || it == '−' }) {
            calculateManualAmount(expression)?.let(::manualAmountText) ?: return expression
        } else {
            expression
        }
        return "$normalized$key"
    }

    val operatorIndex = expression.indexOfLast { it == '+' || it == '−' }
    val operandStart = operatorIndex + 1
    val operand = expression.substring(operandStart)
    if (key == ".") {
        return when {
            operand.contains('.') -> expression
            operand.isEmpty() -> expression + "0."
            else -> expression + key
        }
    }
    if (operand.substringAfter('.', "").length >= 2 && operand.contains('.')) return expression
    if (operand.count(Char::isDigit) >= 9) return expression
    if (operand == "0") {
        return expression.substring(0, operandStart) + key
    }
    return expression + key
}

/**
 * 将一次加减表达式计算为最小货币单位。
 */
internal fun calculateManualAmount(expression: String): Long? {
    val operatorIndex = expression.drop(1).indexOfFirst { it == '+' || it == '−' }
        .takeIf { it >= 0 }
        ?.plus(1)
        ?: return parseMoneyToMinor(expression)
    val left = parseMoneyToMinor(expression.substring(0, operatorIndex)) ?: return null
    val right = parseMoneyToMinor(expression.substring(operatorIndex + 1)) ?: return null
    val result = if (expression[operatorIndex] == '+') left + right else left - right
    return result.takeIf { it > 0 }
}

/**
 * 将最小货币单位还原为适合继续输入的十进制金额。
 */
private fun manualAmountText(amountMinor: Long): String = BigDecimal
    .valueOf(amountMinor, 2)
    .stripTrailingZeros()
    .toPlainString()

/**
 * 将已有账目金额还原为固定两位小数的编辑文本。
 */
internal fun manualFixedAmountText(amountMinor: Long): String = BigDecimal
    .valueOf(amountMinor, 2)
    .setScale(2)
    .toPlainString()

/**
 * 格式化手动记账页显示的创建时间。
 */
private fun formatManualEntryTime(timestamp: Long): String = Instant
    .ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA))

/**
 * 展示自然语言输入、AI 草稿审阅和确认入账流程。
 */
@Composable
fun AiEntryScreen(
    uiState: AccountingUiState,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onCreateDraft: (String) -> Unit,
    onSave: (TransactionDraft, () -> Unit) -> Unit,
) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    SecondaryScaffold(
        title = "AI 智能记账",
        backdrop = backdrop,
        onBack = onBack,
    ) { innerPadding ->
        SecondaryList(innerPadding = innerPadding) {
            item {
                Text(
                    text = "用一句话描述收支，例如“午饭 25 元”或“工资到账 8000 元”。",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            item {
                FormTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "描述这笔账",
                    singleLine = false,
                    maxLines = 5,
                )
            }
            item {
                Button(
                    onClick = { onCreateDraft(input.text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    enabled = input.text.isNotBlank() &&
                        uiState.accounts.isNotEmpty() &&
                        uiState.categories.isNotEmpty(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(text = "生成草稿")
                }
            }
            uiState.aiError?.let { error ->
                item {
                    ValidationText(text = error)
                }
            }
            uiState.aiDraft?.let { draft ->
                item {
                    AiDraftCard(
                        draft = draft,
                        account = uiState.accounts.first { it.id == draft.accountId },
                        category = uiState.categories.first { it.id == draft.categoryId },
                    )
                }
                item {
                    Button(
                        onClick = { onSave(draft, onBack) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(text = "确认并入账")
                    }
                }
                item {
                    Text(
                        text = "AI 结果不会自动写入；请核对金额、类型、账户与分类。",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
        }
    }
}

/**
 * 建立不显示主导航的二级页面 Scaffold。
 */
@Composable
private fun SecondaryScaffold(
    title: String,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    navigationIcon: ImageVector = MiuixIcons.Back,
    navigationContentDescription: String = "返回",
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AccountingBlurTopBar(backdrop = backdrop) {
                SmallTopAppBar(
                    title = title,
                    color = Color.Transparent,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            minWidth = 35.dp,
                            minHeight = 35.dp,
                        ) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = navigationContentDescription,
                            )
                        }
                    },
                )
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                content(innerPadding)
            }
        },
    )
}

/**
 * 建立仅使用顶栏 padding 并自行处理底部导航安全区的二级页列表。
 */
@Composable
private fun SecondaryList(
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
                    modifier = Modifier
                        .height(24.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

/**
 * 展示位于 Card 外部的 MIUIX 文本输入框。
 */
@Composable
private fun FormTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        label = label,
        useLabelAsPlaceholder = true,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        maxLines = maxLines,
    )
}

/**
 * 展示 AI 解析后的只读草稿摘要。
 */
@Composable
private fun AiDraftCard(
    draft: TransactionDraft,
    account: AccountEntity,
    category: CategoryEntity,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = "待确认草稿",
            modifier = Modifier.padding(bottom = 12.dp),
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.title4,
        )
        DraftValue(label = "金额", value = formatMoney(draft.amountMinor))
        DraftValue(
            label = "类型",
            value = if (draft.type == TransactionType.EXPENSE) "支出" else "收入",
        )
        DraftValue(label = "分类", value = category.name)
        DraftValue(label = "账户", value = account.name)
        DraftValue(label = "对象", value = draft.merchant)
        DraftValue(label = "原始描述", value = draft.note)
    }
}

/**
 * 展示 AI 草稿中的一个名称和值。
 */
@Composable
private fun DraftValue(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp),
            textAlign = TextAlign.End,
        )
    }
}

/**
 * 展示表单输入校验错误。
 */
@Composable
private fun ValidationText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        color = MiuixTheme.colorScheme.error,
        style = MiuixTheme.textStyles.footnote1,
    )
}
