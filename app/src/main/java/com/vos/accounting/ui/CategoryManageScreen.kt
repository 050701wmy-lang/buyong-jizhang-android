package com.vos.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.model.TransactionType
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 展示按收支方向分组的分类管理页，支持编辑名称/图标与停用。
 */
@Composable
fun CategoryManageScreen(
    categories: List<CategoryEntity>,
    writeInProgress: Boolean,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onUpdate: (Long, String, String, () -> Unit) -> Unit,
    onArchive: (Long, () -> Unit) -> Unit,
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    SecondaryScaffold(
        title = "分类管理",
        backdrop = backdrop,
        onBack = onBack,
    ) { innerPadding ->
        SecondaryList(innerPadding = innerPadding) {
            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                val items = categories.filter {
                    it.type == type && !it.isArchived
                }
                item {
                    CategoryManageSectionTitle(text = if (type == TransactionType.EXPENSE) "支出" else "收入")
                }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        if (items.isEmpty()) {
                            Text(
                                text = "暂无分类",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                style = MiuixTheme.textStyles.body2,
                            )
                        } else {
                            items.forEach { category ->
                                BasicComponent(
                                    title = category.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    endActions = {
                                        Icon(
                                            imageVector = MiuixIcons.Basic.ArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(width = 10.dp, height = 16.dp),
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    onClick = { editing = category },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { category ->
        CategoryEditDialog(
            category = category,
            writeInProgress = writeInProgress,
            onDismiss = { editing = null },
            onUpdate = { name, iconKey ->
                onUpdate(category.id, name, iconKey) { editing = null }
            },
            onArchive = {
                onArchive(category.id) { editing = null }
            },
        )
    }
}

/**
 * 展示分类编辑弹窗，允许修改名称与图标或停用分类。
 */
@Composable
private fun CategoryEditDialog(
    category: CategoryEntity,
    writeInProgress: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (String, String) -> Unit,
    onArchive: () -> Unit,
) {
    var name by rememberSaveable(category.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(category.name))
    }
    var iconKey by rememberSaveable(category.id) { mutableStateOf(category.iconKey) }
    WindowDialog(
        show = true,
        title = "编辑分类",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = "分类名称",
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onArchive,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "停用")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !writeInProgress,
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = { onUpdate(name.text, iconKey) },
                modifier = Modifier.weight(1f),
                enabled = name.text.isNotBlank() && !writeInProgress,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (writeInProgress) "保存中…" else "保存")
            }
        }
    }
}

/**
 * 展示分类管理页的分区标题。
 */
@Composable
private fun CategoryManageSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 24.dp, bottom = 6.dp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
    )
}
