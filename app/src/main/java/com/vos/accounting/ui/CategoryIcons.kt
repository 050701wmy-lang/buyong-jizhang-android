package com.vos.accounting.ui

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Store

/**
 * 表示分类编辑器中可选择的一个持久化图标。
 */
internal data class CategoryIconOption(
    val key: String,
    val icon: ImageVector,
)

internal val categoryIconOptions = listOf(
    CategoryIconOption("store", MiuixIcons.Store),
    CategoryIconOption("carrier", MiuixIcons.Carrier),
    CategoryIconOption("bank_cards", MiuixIcons.BankCards),
    CategoryIconOption("home", MiuixIcons.Home),
    CategoryIconOption("music", MiuixIcons.Music),
    CategoryIconOption("promotions", MiuixIcons.Promotions),
    CategoryIconOption("more", MiuixIcons.More),
)

/**
 * 返回真实分类名称对应的 MIUIX 图标。
 */
internal fun accountingCategoryIcon(name: String): ImageVector = when (name) {
    "餐饮" -> MiuixIcons.Store
    "交通" -> MiuixIcons.Carrier
    "购物" -> MiuixIcons.BankCards
    "居住" -> MiuixIcons.Home
    "娱乐" -> MiuixIcons.Music
    "工资" -> MiuixIcons.BankCards
    "奖金" -> MiuixIcons.Promotions
    else -> MiuixIcons.More
}

/**
 * 返回持久化图标键对应的 MIUIX 图标，并兼容旧分类名称。
 */
internal fun accountingCategoryIcon(
    iconKey: String,
    fallbackName: String,
): ImageVector = categoryIconOptions
    .firstOrNull { it.key == iconKey }
    ?.icon
    ?: accountingCategoryIcon(fallbackName)
