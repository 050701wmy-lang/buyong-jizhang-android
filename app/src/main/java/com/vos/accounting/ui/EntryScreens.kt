package com.vos.accounting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountLedgerCrossRef
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.LedgerRecord
import com.vos.accounting.data.TransactionRecord
import com.vos.accounting.data.convertCurrencyMinor
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.MAX_AMOUNT_MINOR
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowBottomSheet
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
    onAddLedger: () -> Unit,
    onManageLedgers: () -> Unit,
    onAddAccount: (Long) -> Unit,
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
    var accountAmountExpression by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.accountAmountMinor?.let(::manualFixedAmountText).orEmpty())
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
    var currencyKey by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.currencyKey.orEmpty())
    }
    var currencyManuallySelected by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction != null)
    }
    var editingAccountAmount by rememberSaveable(transaction?.id) { mutableStateOf(false) }
    var conversionInitialized by rememberSaveable(transaction?.id) { mutableStateOf(false) }
    var ledgerId by rememberSaveable(transaction?.id, initialAccountId) {
        mutableStateOf(
            transaction?.ledgerId
                ?: uiState.currentLedgerId.takeIf { currentLedgerId ->
                    initialAccountId == 0L || uiState.accountLedgerCrossRefs.any {
                        it.accountId == initialAccountId && it.ledgerId == currentLedgerId
                    }
                }
                ?: uiState.accountLedgerCrossRefs.firstOrNull {
                    it.accountId == initialAccountId
                }?.ledgerId
                ?: uiState.currentLedgerId,
        )
    }
    var observedCurrentLedgerId by rememberSaveable(transaction?.id) {
        mutableStateOf(uiState.currentLedgerId)
    }
    var categoryId by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.categoryId ?: 0L)
    }
    var keypadVisible by rememberSaveable(transaction?.id) {
        mutableStateOf(false)
    }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showLedgerPicker by rememberSaveable { mutableStateOf(false) }
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }
    var occurredAt by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.occurredAt ?: System.currentTimeMillis())
    }
    val matchingCategories = uiState.categories.filter {
        it.type == type && (!it.isArchived || it.id == transaction?.categoryId)
    }
    val selectableLedgers = uiState.ledgers.filter {
        !it.isHidden || it.id == transaction?.ledgerId
    }
    val linkedAccountIds = uiState.accountLedgerCrossRefs
        .filter { it.ledgerId == ledgerId }
        .mapTo(mutableSetOf(), AccountLedgerCrossRef::accountId)
    val selectableAccounts = uiState.accounts.filter {
        it.id in linkedAccountIds && (!it.isArchived || it.id == transaction?.accountId)
    }
    val amountMinor = calculateManualAmount(amountExpression)
    val selectedAccount = uiState.accounts.firstOrNull { it.id == accountId }
    val accountCurrency = uiState.currencies.firstOrNull { it.key == selectedAccount?.currencyKey }
    val transactionCurrency = uiState.currencies.firstOrNull { it.key == currencyKey }
    val usesAccountCurrency = transactionCurrency?.key == accountCurrency?.key
    val accountAmountMinor = if (usesAccountCurrency) amountMinor else calculateManualAmount(accountAmountExpression)
    val currencySymbol = transactionCurrency?.symbol ?: accountCurrency?.symbol ?: "¥"

    LaunchedEffect(selectableLedgers) {
        if (selectableLedgers.none { it.id == ledgerId }) {
            ledgerId = selectableLedgers.firstOrNull()?.id ?: 0
        }
    }
    LaunchedEffect(uiState.currentLedgerId) {
        if (
            transaction == null &&
            uiState.currentLedgerId != observedCurrentLedgerId &&
            selectableLedgers.any { it.id == uiState.currentLedgerId }
        ) {
            ledgerId = uiState.currentLedgerId
        }
        observedCurrentLedgerId = uiState.currentLedgerId
    }
    LaunchedEffect(selectableAccounts) {
        if (selectableAccounts.none { it.id == accountId }) {
            accountId = selectableAccounts.firstOrNull(AccountEntity::isDefault)?.id
                ?: selectableAccounts.firstOrNull()?.id
                ?: 0
        }
    }
    LaunchedEffect(accountId, accountCurrency?.key) {
        if (transaction == null && !currencyManuallySelected) {
            currencyKey = accountCurrency?.key.orEmpty()
        }
    }
    LaunchedEffect(amountMinor, currencyKey, accountCurrency?.key) {
        if (amountMinor == null || transactionCurrency == null || accountCurrency == null) {
            return@LaunchedEffect
        }
        if (transaction != null && !conversionInitialized) {
            conversionInitialized = true
        } else {
            conversionInitialized = true
            if (!usesAccountCurrency) {
                accountAmountExpression = runCatching {
                    convertCurrencyMinor(
                        amountMinor,
                        transactionCurrency.rateToCnyScaled,
                        accountCurrency.rateToCnyScaled,
                    )
                }.getOrNull()?.let(::manualFixedAmountText).orEmpty()
            }
        }
    }
    LaunchedEffect(type, matchingCategories) {
        if (matchingCategories.none { it.id == categoryId }) {
            categoryId = matchingCategories.firstOrNull()?.id ?: 0
        }
    }

    SecondaryScaffold(
        title = selectableLedgers.firstOrNull { it.id == ledgerId }?.name.orEmpty(),
        backdrop = backdrop,
        onBack = onBack,
        collapsible = false,
        onTitleClick = { showLedgerPicker = true },
        navigationIcon = MiuixIcons.Close,
        navigationContentDescription = "关闭",
    ) { innerPadding ->
        ManualEntryContent(
            innerPadding = innerPadding,
            amountExpression = amountExpression,
            amountMinor = amountMinor,
            currencySymbol = currencySymbol,
            type = type,
            categories = matchingCategories,
            categoryId = categoryId,
            accounts = selectableAccounts,
            accountId = accountId,
            currencyName = transactionCurrency?.name.orEmpty(),
            accountAmountText = if (usesAccountCurrency) null else {
                "${accountCurrency?.symbol.orEmpty()} ${accountAmountExpression.ifBlank { "0.00" }}"
            },
            note = note,
            merchant = merchant,
            occurredAt = occurredAt,
            canSave = amountMinor != null &&
                selectableLedgers.any { it.id == ledgerId } &&
                selectableAccounts.any { it.id == accountId } &&
                matchingCategories.any { it.id == categoryId } &&
                transactionCurrency != null &&
                accountAmountMinor != null &&
                !uiState.writeInProgress,
            writeInProgress = uiState.writeInProgress,
            editMode = transaction != null,
            keypadVisible = keypadVisible,
            onSelectType = { type = it },
            onSelectCategory = { categoryId = it },
            onSelectAccount = { accountId = it },
            onSelectCurrency = { showCurrencyPicker = true },
            onEditAccountAmount = {
                editingAccountAmount = true
                keypadVisible = true
            },
            onAddAccount = { onAddAccount(ledgerId) },
            onAddCategory = { showCategoryDialog = true },
            onNoteChange = { note = it },
            onMerchantChange = { merchant = it },
            onOccurredAtChange = { occurredAt = it },
            onShowKeypad = {
                editingAccountAmount = false
                keypadVisible = true
            },
            onHideKeypad = { keypadVisible = false },
            onAmountKey = {
                if (editingAccountAmount) {
                    accountAmountExpression = appendManualAmountKey(accountAmountExpression, it)
                } else {
                    amountExpression = appendManualAmountKey(amountExpression, it)
                }
            },
            onAmountDelete = {
                if (editingAccountAmount) {
                    accountAmountExpression = accountAmountExpression.dropLast(1)
                } else {
                    amountExpression = amountExpression.dropLast(1)
                }
            },
            onDelete = { showDeleteDialog = true },
            onSave = {
                amountMinor?.let {
                    val draft = TransactionDraft(
                        type = type,
                        amountMinor = it,
                        currencyKey = currencyKey,
                        accountAmountMinor = accountAmountMinor ?: return@let,
                        accountId = accountId,
                        categoryId = categoryId,
                        merchant = merchant.text,
                        note = note.text,
                        occurredAt = occurredAt,
                        source = transaction?.source ?: TransactionSource.MANUAL,
                        ledgerId = ledgerId,
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
    ManualLedgerPickerSheet(
        show = showLedgerPicker,
        ledgers = selectableLedgers,
        selectedId = ledgerId,
        onDismiss = { showLedgerPicker = false },
        onSelect = {
            ledgerId = it
            showLedgerPicker = false
        },
        onAdd = {
            showLedgerPicker = false
            onAddLedger()
        },
        onManage = {
            showLedgerPicker = false
            onManageLedgers()
        },
    )
    ManualCurrencyPickerSheet(
        show = showCurrencyPicker,
        currencies = uiState.currencies,
        selectedKey = currencyKey,
        onDismiss = { showCurrencyPicker = false },
        onSelect = {
            currencyManuallySelected = true
            currencyKey = it
            showCurrencyPicker = false
        },
    )
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
    currencySymbol: String,
    type: TransactionType,
    categories: List<CategoryEntity>,
    categoryId: Long,
    accounts: List<AccountEntity>,
    accountId: Long,
    currencyName: String,
    accountAmountText: String?,
    note: TextFieldValue,
    merchant: TextFieldValue,
    occurredAt: Long,
    canSave: Boolean,
    writeInProgress: Boolean,
    editMode: Boolean,
    keypadVisible: Boolean,
    onSelectType: (TransactionType) -> Unit,
    onSelectCategory: (Long) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectCurrency: () -> Unit,
    onEditAccountAmount: () -> Unit,
    onAddAccount: () -> Unit,
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
    val focusManager = LocalFocusManager.current
    val dismissKeypadInteractionSource = remember { MutableInteractionSource() }
    var noteFocused by rememberSaveable { mutableStateOf(false) }
    var merchantFocused by rememberSaveable { mutableStateOf(false) }
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
                .clickable(
                    interactionSource = dismissKeypadInteractionSource,
                    indication = null,
                    onClick = onHideKeypad,
                )
                .verticalScroll(rememberScrollState()),
        ) {
            ManualTypeTabs(
                type = type,
                onSelectType = {
                    onHideKeypad()
                    onSelectType(it)
                },
            )
            ManualAmountDisplay(
                amountExpression = amountExpression,
                currencySymbol = currencySymbol,
                invalid = amountExpression.isNotBlank() && amountMinor == null,
                onClick = {
                    focusManager.clearFocus()
                    onShowKeypad()
                },
            )
            ManualCategoryGrid(
                categories = categories,
                selectedId = categoryId,
                onSelect = {
                    onHideKeypad()
                    onSelectCategory(it)
                },
                onAdd = {
                    onHideKeypad()
                    onAddCategory()
                },
            )
            ManualDetailRows(
                accounts = accounts,
                accountId = accountId,
                currencyName = currencyName,
                accountAmountText = accountAmountText,
                occurredAt = occurredAt,
                note = note,
                merchant = merchant,
                onOccurredAtChange = onOccurredAtChange,
                onSelectAccount = onSelectAccount,
                onSelectCurrency = onSelectCurrency,
                onEditAccountAmount = onEditAccountAmount,
                onAddAccount = onAddAccount,
                onNoteChange = onNoteChange,
                onMerchantChange = onMerchantChange,
                onNoteFocusChange = { noteFocused = it },
                onMerchantFocusChange = { merchantFocused = it },
                onInteraction = onHideKeypad,
            )
        }
        if (!noteFocused && !merchantFocused && keypadVisible) {
            ManualKeypad(
                onAmountKey = onAmountKey,
                onDelete = onAmountDelete,
                onSave = onSave,
                saveEnabled = canSave,
                writeInProgress = writeInProgress,
            )
        } else if (editMode) {
            ManualEditActions(
                saveEnabled = canSave,
                writeInProgress = writeInProgress,
                onDelete = onDelete,
                onSave = onSave,
            )
        } else {
            ManualSaveAction(
                saveEnabled = canSave,
                writeInProgress = writeInProgress,
                onSave = onSave,
            )
        }
    }
    LaunchedEffect(noteFocused, merchantFocused) {
        if (noteFocused || merchantFocused) onHideKeypad()
    }
}

/**
 * 展示与参考界面一致的左对齐支出与收入类型标签。
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
            modifier = Modifier.width(78.dp),
            title = "支出",
            selected = type == TransactionType.EXPENSE,
            onClick = { onSelectType(TransactionType.EXPENSE) },
        )
        ManualTypeTab(
            modifier = Modifier.width(78.dp),
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
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
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
                MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.55f)
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
    currencySymbol: String,
    invalid: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currencySymbol,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            Text(
                text = amountExpression.ifBlank { "0.00" },
                modifier = Modifier.padding(start = 6.dp),
                color = if (amountExpression.isBlank()) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.28f)
                } else {
                    MiuixTheme.colorScheme.onBackground
                },
                fontWeight = FontWeight.Medium,
                fontSize = 40.sp,
                maxLines = 1,
            )
        }
        HorizontalDivider()
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
 * 按每页五行两列分页展示当前收支类型的真实分类，并在末尾追加"添加"入口。
 */
@Composable
private fun ManualCategoryGrid(
    categories: List<CategoryEntity>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val itemsPerPage = 10
    val totalCount = categories.size + 1
    val pageCount = maxOf(1, (totalCount + itemsPerPage - 1) / itemsPerPage)
    val pagerState = rememberPagerState { pageCount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(2) { rowIndex ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(5) { columnIndex ->
                            val itemIndex = pageIndex * itemsPerPage + rowIndex * 5 + columnIndex
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
                                itemIndex == categories.size -> {
                                    ManualCategoryAddItem(
                                        modifier = Modifier.weight(1f),
                                        onClick = onAdd,
                                    )
                                }
                                else -> Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(6.dp)
                        .squircleBackground(
                            color = if (index == pagerState.currentPage) {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            } else {
                                MiuixTheme.colorScheme.surfaceContainerHigh
                            },
                            cornerRadius = 3.dp,
                        ),
                )
            }
        }
    }
}

/**
 * 展示一个带 MIUIX 图标和 squircle 选中态的分类宫格项。
 */
@Composable
private fun ManualCategoryItem(
    modifier: Modifier,
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconOption = categoryIconOption(category.iconKey, category.name)
    val iconContainerModifier = if (selected) {
        Modifier
            .size(52.dp)
            .squircleBackground(
                color = MiuixTheme.colorScheme.primary,
                cornerRadius = 15.dp,
            )
            .squircleClip(15.dp)
    } else {
        Modifier
            .size(52.dp)
            .squircleBackground(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                cornerRadius = 15.dp,
            )
            .padding(1.dp)
            .squircleBackground(
                color = MiuixTheme.colorScheme.surface,
                cornerRadius = 14.dp,
            )
            .squircleClip(14.dp)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = iconContainerModifier.clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            CategoryIcon(
                option = iconOption,
                modifier = Modifier.size(28.dp),
                tint = if (selected) Color.White else MiuixTheme.colorScheme.primary,
            )
        }
        Text(
            text = category.name,
            modifier = Modifier.padding(top = 6.dp),
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
 * 展示分类宫格末尾的"添加"图标入口，与分类项同尺寸。
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
                .size(52.dp)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.surfaceContainerHigh,
                    cornerRadius = 15.dp,
                )
                .padding(1.dp)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.surface,
                    cornerRadius = 14.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Add,
                contentDescription = "添加分类",
                modifier = Modifier.size(26.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Text(
            text = "添加",
            modifier = Modifier.padding(top = 6.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

/**
 * 显示手动记账页的账本或账户单选入口。
 */
@Composable
private fun ManualSelectionRow(
    icon: ImageVector,
    title: String,
    value: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
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
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp),
            color = if (isPlaceholder) {
                MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.45f)
            } else {
                MiuixTheme.colorScheme.onBackground
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body2,
        )
        Icon(
            imageVector = MiuixIcons.Basic.ArrowRight,
            contentDescription = null,
            modifier = Modifier.size(width = 10.dp, height = 16.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
    }
}

/**
 * 以连续账户列表展示手动记账页的账户选择弹层。
 */
@Composable
private fun ManualAccountPickerSheet(
    show: Boolean,
    accounts: List<AccountEntity>,
    selectedId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = "选择账户",
        endAction = {
            Row(modifier = Modifier.padding(end = 20.dp)) {
                TopBarIconAction(MiuixIcons.Add, "添加账户", onAdd)
            }
        },
        onDismissRequest = onDismiss,
        cornerRadius = 30.dp,
        insideMargin = DpSize(0.dp, 20.dp),
        allowDismiss = true,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp),
                insideMargin = PaddingValues(0.dp),
            ) {
                accounts.forEach { account ->
                    BasicComponent(
                        title = account.name,
                        startAction = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .squircleBackground(
                                        color = MiuixTheme.colorScheme.secondaryContainer,
                                        cornerRadius = 12.dp,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                AccountIcon(
                                    iconKey = account.iconKey,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                        endActions = {
                            if (account.id == selectedId) {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = "当前账户",
                                    modifier = Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { onSelect(account.id) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp).navigationBarsPadding())
        }
    }
}

/** 以连续币种列表展示手动记账页的交易币种选择弹层。 */
@Composable
private fun ManualCurrencyPickerSheet(
    show: Boolean,
    currencies: List<CurrencyEntity>,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = "选择交易币种",
        onDismissRequest = onDismiss,
        cornerRadius = 30.dp,
        insideMargin = DpSize(0.dp, 20.dp),
        allowDismiss = true,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp),
                insideMargin = PaddingValues(0.dp),
            ) {
                currencies.forEach { currency ->
                    BasicComponent(
                        title = "${currency.symbol}  ${currency.name}",
                        summary = currency.code.ifBlank { currency.symbol },
                        endActions = {
                            if (currency.key == selectedKey) {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = "当前交易币种",
                                    modifier = Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { onSelect(currency.key) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp).navigationBarsPadding())
        }
    }
}

/**
 * 以账本封面网格展示手动记账页的账本选择弹层。
 */
@Composable
private fun ManualLedgerPickerSheet(
    show: Boolean,
    ledgers: List<LedgerRecord>,
    selectedId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
    onManage: () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = "选择账本",
        endAction = {
            Row(
                modifier = Modifier.padding(end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TopBarIconAction(MiuixIcons.Add, "添加账本", onAdd)
                TopBarIconAction(MiuixIcons.Settings, "账本管理", onManage)
            }
        },
        onDismissRequest = onDismiss,
        cornerRadius = 30.dp,
        insideMargin = DpSize(0.dp, 20.dp),
        allowDismiss = true,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                LedgerGrid(
                    ledgers = ledgers,
                    currentLedgerId = selectedId,
                    onEdit = null,
                    onSelect = onSelect,
                )
            }
            Spacer(Modifier.height(12.dp).navigationBarsPadding())
        }
    }
}

/**
 * 按参考界面展示账户、时间和备注等附加信息。
 */
@Composable
private fun ManualDetailRows(
    accounts: List<AccountEntity>,
    accountId: Long,
    currencyName: String,
    accountAmountText: String?,
    occurredAt: Long,
    note: TextFieldValue,
    merchant: TextFieldValue,
    onOccurredAtChange: (Long) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectCurrency: () -> Unit,
    onEditAccountAmount: () -> Unit,
    onAddAccount: () -> Unit,
    onNoteChange: (TextFieldValue) -> Unit,
    onMerchantChange: (TextFieldValue) -> Unit,
    onNoteFocusChange: (Boolean) -> Unit,
    onMerchantFocusChange: (Boolean) -> Unit,
    onInteraction: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    var showAccountPicker by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        HorizontalDivider()
        ManualSelectionRow(
            icon = MiuixIcons.Store,
            title = "账户",
            value = accounts.firstOrNull { it.id == accountId }?.name ?: "当前账本暂无账户",
            isPlaceholder = accounts.isEmpty(),
            onClick = {
                onInteraction()
                showAccountPicker = true
            },
        )
        HorizontalDivider()
        ManualSelectionRow(
            icon = MiuixIcons.Basic.ArrowUpDown,
            title = "币种",
            value = currencyName,
            isPlaceholder = currencyName.isEmpty(),
            onClick = {
                onInteraction()
                onSelectCurrency()
            },
        )
        if (accountAmountText != null) {
            HorizontalDivider()
            ManualSelectionRow(
                icon = MiuixIcons.Basic.ArrowUpDown,
                title = "账户金额",
                value = accountAmountText,
                isPlaceholder = false,
                onClick = {
                    onInteraction()
                    onEditAccountAmount()
                },
            )
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable {
                    onInteraction()
                    showDateTimePicker = true
                },
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
                style = MiuixTheme.textStyles.body2,
            )
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                modifier = Modifier.size(width = 10.dp, height = 16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
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
                imageVector = MiuixIcons.Store,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "对象",
                modifier = Modifier.padding(start = 8.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            BasicTextField(
                value = merchant,
                onValueChange = onMerchantChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp)
                    .onFocusChanged { onMerchantFocusChange(it.isFocused) },
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
                        if (merchant.text.isEmpty()) {
                            Text(
                                text = "点击添加对象",
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
    ManualAccountPickerSheet(
        show = showAccountPicker,
        accounts = accounts,
        selectedId = accountId,
        onDismiss = { showAccountPicker = false },
        onSelect = {
            onSelectAccount(it)
            showAccountPicker = false
        },
        onAdd = {
            showAccountPicker = false
            onAddAccount()
        },
    )
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
 * 使用 MIUIX 底部弹层分步编辑账目的本地日期与时间。
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
    var editingTime by rememberSaveable(show) { mutableStateOf(false) }
    val pickerColors = NumberPickerDefaults.colors(
        selectedTextColor = MiuixTheme.colorScheme.primary,
    )
    val pickerTextStyle = MiuixTheme.textStyles.title3.copy(
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium,
    )
    val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
    LaunchedEffect(maxDay) {
        day = day.coerceAtMost(maxDay)
    }
    WindowBottomSheet(
        show = show,
        title = "编辑日期",
        onDismissRequest = onDismiss,
        cornerRadius = 30.dp,
        insideMargin = DpSize(24.dp, 20.dp),
        allowDismiss = true,
    ) {
        Column {
            HorizontalDivider()
            if (editingTime) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable { editingTime = false },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "日期",
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.title4,
                    )
                    Text(
                        text = "${year}年${month}月${day}日",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body1,
                    )
                    Icon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth()) {
                    NumberPicker(
                        value = hour,
                        onValueChange = { hour = it },
                        modifier = Modifier.weight(1f),
                        range = 0..23,
                        label = { "%02d时".format(it) },
                        visibleItemCount = 3,
                        wrapAround = true,
                        colors = pickerColors,
                        textStyle = pickerTextStyle,
                    )
                    NumberPicker(
                        value = minute,
                        onValueChange = { minute = it },
                        modifier = Modifier.weight(1f),
                        range = 0..59,
                        label = { "%02d分".format(it) },
                        visibleItemCount = 3,
                        wrapAround = true,
                        colors = pickerColors,
                        textStyle = pickerTextStyle,
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    NumberPicker(
                        value = year,
                        onValueChange = { year = it },
                        modifier = Modifier.weight(1.2f),
                        range = 1970..2100,
                        label = { "${it}年" },
                        visibleItemCount = 3,
                        colors = pickerColors,
                        textStyle = pickerTextStyle,
                    )
                    NumberPicker(
                        value = month,
                        onValueChange = { month = it },
                        modifier = Modifier.weight(1f),
                        range = 1..12,
                        label = { "${it}月" },
                        visibleItemCount = 3,
                        wrapAround = true,
                        colors = pickerColors,
                        textStyle = pickerTextStyle,
                    )
                    NumberPicker(
                        value = day,
                        onValueChange = { day = it },
                        modifier = Modifier.weight(1f),
                        range = 1..maxDay,
                        label = { "${it}日" },
                        visibleItemCount = 3,
                        wrapAround = true,
                        colors = pickerColors,
                        textStyle = pickerTextStyle,
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable { editingTime = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "时刻",
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.title4,
                    )
                    Text(
                        text = "%02d:%02d".format(hour, minute),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body1,
                    )
                    Icon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
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
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
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
 * 展示新建记账页固定在底部的通栏保存按钮。
 */
@Composable
private fun ManualSaveAction(
    saveEnabled: Boolean,
    writeInProgress: Boolean,
    onSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = saveEnabled,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(
                text = if (writeInProgress) "保存中" else "保存",
                style = MiuixTheme.textStyles.title4,
            )
        }
    }
}

/**
 * 展示新增分类的名称与图标选择底部弹层。
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
    WindowBottomSheet(
        show = show,
        title = "添加分类",
        endAction = {
            Row(modifier = Modifier.padding(end = 20.dp)) {
                IconButton(
                    onClick = {
                        onAdd(
                            name.text,
                            iconKey,
                            onCreated,
                            { duplicate = true },
                        )
                    },
                    minWidth = 35.dp,
                    minHeight = 35.dp,
                    enabled = name.text.isNotBlank() && !writeInProgress,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Ok,
                        contentDescription = "确认添加分类",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        cornerRadius = 30.dp,
        insideMargin = DpSize(20.dp, 20.dp),
        allowDismiss = !writeInProgress,
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
                        val selected = option.key == iconKey
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .squircleBackground(
                                    color = if (selected) {
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
                            CategoryIcon(
                                option = option,
                                modifier = Modifier.size(27.dp),
                                tint = if (selected) Color.White else MiuixTheme.colorScheme.primary,
                            )
                        }
                    }
                    repeat(5 - rowIcons.size) {
                        Spacer(modifier = Modifier.size(50.dp))
                    }
                }
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
    accountTypes: List<AccountTypeEntity>,
    currencies: List<CurrencyEntity>,
    ledgers: List<LedgerRecord>,
    selectedLedgerIds: Set<Long>,
    selectedTypeKey: String,
    selectedCurrencyKey: String,
    selectedIconKey: String,
    currentBalanceMinor: Long,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onBackRequestChange: ((() -> Unit)?) -> Unit,
    onSave: (AccountEntity, Set<Long>, () -> Unit) -> Unit,
    onDelete: (Long, () -> Unit) -> Unit,
    onOpenTypePicker: (String) -> Unit,
    onOpenCurrencyPicker: (String) -> Unit,
    onOpenIconPicker: (String) -> Unit,
    onOpenLedgerPicker: (Set<Long>) -> Unit,
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
    var typeKey by rememberSaveable(account?.id) {
        mutableStateOf(selectedTypeKey)
    }
    var iconKey by rememberSaveable(account?.id) {
        mutableStateOf(selectedIconKey)
    }
    var currencyKey by rememberSaveable(account?.id) {
        mutableStateOf(selectedCurrencyKey)
    }
    var isDefault by rememberSaveable(account?.id) {
        mutableStateOf(account?.isDefault ?: false)
    }
    var isArchived by rememberSaveable(account?.id) {
        mutableStateOf(account?.isArchived ?: false)
    }
    val initialName = rememberSaveable(account?.id) { account?.name.orEmpty() }
    val initialBalance = rememberSaveable(account?.id) {
        if (account == null) "" else manualAmountText(currentBalanceMinor)
    }
    val initialTypeKey = rememberSaveable(account?.id) { selectedTypeKey }
    val initialIconKey = rememberSaveable(account?.id) { selectedIconKey }
    val initialCurrencyKey = rememberSaveable(account?.id) { selectedCurrencyKey }
    val initialIsDefault = rememberSaveable(account?.id) { account?.isDefault ?: false }
    val initialIsArchived = rememberSaveable(account?.id) { account?.isArchived ?: false }
    val initialLedgerIds = rememberSaveable(account?.id) { selectedLedgerIds.toList() }
    var showBalanceDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(account?.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(account?.id) { mutableStateOf(false) }
    val balanceMinor = parseSignedMoneyToMinor(balance.text)
    val transactionNetMinor = if (account == null) {
        0
    } else {
        currentBalanceMinor - account.openingBalanceMinor
    }
    val canSave = name.text.isNotBlank() && balanceMinor != null && selectedLedgerIds.isNotEmpty() && !writeInProgress
    val hasUnsavedChanges = name.text != initialName ||
        balance.text != initialBalance ||
        typeKey != initialTypeKey ||
        iconKey != initialIconKey ||
        currencyKey != initialCurrencyKey ||
        isDefault != initialIsDefault ||
        isArchived != initialIsArchived ||
        selectedLedgerIds != initialLedgerIds.toSet()
    val requestBack = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }
    val latestRequestBack by rememberUpdatedState(requestBack)
    DisposableEffect(Unit) {
        onBackRequestChange { latestRequestBack() }
        onDispose { onBackRequestChange(null) }
    }
    val scrollBehavior = MiuixScrollBehavior()
    LaunchedEffect(selectedIconKey) {
        iconKey = selectedIconKey
    }
    LaunchedEffect(selectedTypeKey, accountTypes) {
        val selectedType = accountTypes.firstOrNull { it.key == selectedTypeKey }
        if (selectedType != null) {
            if (iconKey == defaultAccountIconKey(type)) {
                iconKey = selectedType.iconKey
            }
            type = selectedType.baseType
            typeKey = selectedType.key
        }
    }
    LaunchedEffect(selectedCurrencyKey) {
        currencyKey = selectedCurrencyKey
    }
    val saveAccount = {
        onSave(
            (account ?: AccountEntity(
                name = "",
                type = type,
                typeKey = typeKey,
                currencyKey = currencyKey,
                openingBalanceMinor = 0,
                sortOrder = 0,
            )).copy(
                name = name.text,
                type = type,
                typeKey = typeKey,
                currencyKey = currencyKey,
                iconKey = iconKey,
                openingBalanceMinor = (balanceMinor ?: 0) - transactionNetMinor,
                isDefault = isDefault,
                isArchived = isArchived,
            ),
            selectedLedgerIds,
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
                    onBack = requestBack,
                    onSave = saveAccount,
                )
            },
            bottomBar = {
                AccountEditorActions(
                    account = account,
                    canSave = canSave,
                    writeInProgress = writeInProgress,
                    backdrop = backdrop,
                    onDelete = { showDeleteDialog = true },
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
                            BasicComponent(
                                title = "账户类型",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Row(
                                        modifier = Modifier.height(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = accountTypes.firstOrNull { it.key == typeKey }?.name.orEmpty(),
                                            modifier = Modifier.offset(y = (-1).dp),
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            style = MiuixTheme.textStyles.body2,
                                        )
                                        Icon(
                                            imageVector = MiuixIcons.Basic.ArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(start = 6.dp)
                                                .size(width = 10.dp, height = 16.dp),
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    }
                                },
                                onClick = { onOpenTypePicker(typeKey) },
                            )
                            BasicComponent(
                                title = "账户图标",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Row(
                                        modifier = Modifier.height(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AccountIcon(
                                            iconKey = iconKey,
                                            modifier = Modifier
                                                .offset(y = (-1).dp)
                                                .size(24.dp),
                                        )
                                        Icon(
                                            imageVector = MiuixIcons.Basic.ArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(width = 10.dp, height = 16.dp),
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    }
                                },
                                onClick = { onOpenIconPicker(iconKey) },
                            )
                            BasicComponent(
                                title = "账户币种",
                                modifier = Modifier.fillMaxWidth(),
                                endActions = {
                                    Text(
                                        text = currencies.firstOrNull { it.key == currencyKey }?.name.orEmpty(),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        style = MiuixTheme.textStyles.body2,
                                    )
                                    Icon(
                                        imageVector = MiuixIcons.Basic.ArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .size(width = 10.dp, height = 16.dp),
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                },
                                onClick = { onOpenCurrencyPicker(currencyKey) },
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
                        AccountSectionTitle("账户设置", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp), fontWeight = FontWeight.Medium)
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
                                    Row(modifier = Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (selectedLedgerIds.size == ledgers.size) "全部" else "${selectedLedgerIds.size} 个",
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            style = MiuixTheme.textStyles.body2,
                                        )
                                        Icon(
                                            imageVector = MiuixIcons.Basic.ArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.padding(start = 6.dp).size(width = 10.dp, height = 16.dp),
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    }
                                },
                                onClick = { onOpenLedgerPicker(selectedLedgerIds) },
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
    AccountEditorDiscardDialog(
        show = showDiscardDialog,
        onDismiss = { showDiscardDialog = false },
        onConfirm = {
            showDiscardDialog = false
            onBack()
        },
    )
    AccountDeleteDialog(
        account = account,
        show = showDeleteDialog,
        writeInProgress = writeInProgress,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            account?.let { onDelete(it.id, onBack) }
        },
    )
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
 * 展示账户编辑页固定在底部的删除与保存操作。
 */
@Composable
private fun AccountEditorActions(
    account: AccountEntity?,
    canSave: Boolean,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onDelete: () -> Unit,
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
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = if (writeInProgress) "删除中…" else "删除")
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

/** 确认是否放弃账户编辑页中尚未保存的更改。 */
@Composable
private fun AccountEditorDiscardDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = "放弃编辑",
        summary = "要放弃您所做的更改吗？",
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = "放弃")
            }
        }
    }
}

/** 二次确认删除没有历史账目的账户。 */
@Composable
private fun AccountDeleteDialog(
    account: AccountEntity?,
    show: Boolean,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(
        show = show && account != null,
        title = "删除账户",
        summary = account?.let { "确定删除“${it.name}”吗？" },
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
                Text(text = if (writeInProgress) "删除中…" else "删除")
            }
        }
    }
}

/**
 * 展示可复用的 HyperOS 弹出单选项。
 */
@Composable
internal fun PopupSelectionRow(
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
    }?.takeIf { kotlin.math.abs(it) <= MAX_AMOUNT_MINOR }
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
        Row(Modifier.fillMaxWidth()) {
            ManualKeypadButton("1", Modifier.weight(1f)) { onAmountKey("1") }
            ManualKeypadButton("2", Modifier.weight(1f)) { onAmountKey("2") }
            ManualKeypadButton("3", Modifier.weight(1f)) { onAmountKey("3") }
            ManualKeypadButton(text = "⌫", modifier = Modifier.weight(1f), backgroundColor = MiuixTheme.colorScheme.secondaryContainer, textSize = 24.sp, onClick = onDelete)
        }
        Row(Modifier.fillMaxWidth()) {
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
        Row(Modifier.fillMaxWidth()) {
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
        Row(Modifier.fillMaxWidth()) {
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
    val result = try {
        if (expression[operatorIndex] == '+') Math.addExact(left, right) else Math.subtractExact(left, right)
    } catch (_: ArithmeticException) {
        return null
    }
    return result.takeIf { it > 0 && it <= MAX_AMOUNT_MINOR }
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
 * 建立不显示主导航的二级页面 Scaffold。
 */
@Composable
internal fun SecondaryScaffold(
    title: String,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    collapsible: Boolean = true,
    onTitleClick: (() -> Unit)? = null,
    navigationIcon: ImageVector = MiuixIcons.Back,
    navigationContentDescription: String = "返回",
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AccountingBlurTopBar(backdrop = backdrop) {
                    val topBarNavigationIcon: @Composable () -> Unit = {
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
                    }
                    Box {
                        if (isWide || !collapsible) {
                            SmallTopAppBar(
                                title = if (onTitleClick == null) title else "",
                                color = Color.Transparent,
                                navigationIcon = topBarNavigationIcon,
                                actions = actions,
                                actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                                scrollBehavior = scrollBehavior,
                            )
                        } else {
                            TopAppBar(
                                title = if (onTitleClick == null) title else "",
                                color = Color.Transparent,
                                navigationIcon = topBarNavigationIcon,
                                actions = actions,
                                actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                                scrollBehavior = scrollBehavior,
                            )
                        }
                        onTitleClick?.let { clickTitle ->
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(
                                        top = WindowInsets.statusBars
                                            .asPaddingValues()
                                            .calculateTopPadding(),
                                    )
                                    .height(TopAppBarDefaults.CollapsedHeight)
                                    .clickable(onClick = clickTitle)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Medium,
                                    style = MiuixTheme.textStyles.title3,
                                )
                                Icon(
                                    imageVector = MiuixIcons.Basic.ArrowRight,
                                    contentDescription = "切换账本",
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(width = 10.dp, height = 16.dp)
                                        .rotate(90f),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = { bottomBar?.invoke() },
            content = { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    content(innerPadding)
                }
            },
        )
    }
}

/**
 * 建立仅使用顶栏 padding 并自行处理底部导航安全区的二级页列表。
 */
@Composable
internal fun SecondaryList(
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
