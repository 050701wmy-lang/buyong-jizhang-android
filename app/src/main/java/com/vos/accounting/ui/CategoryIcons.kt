package com.vos.accounting.ui

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.MapAlbum
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Store

/**
 * 表示分类编辑器中可选择的一个持久化图标，colorful 为 true 时保持原彩色不被主题色覆盖。
 */
internal data class CategoryIconOption(
    val key: String,
    val icon: ImageVector,
    val colorful: Boolean = false,
)

/**
 * 内置 MIUIX 分类图标集合，作为自定义图标之外的补充选项。
 */
private val miuiCategoryIconOptions = listOf(
    CategoryIconOption("store", MiuixIcons.Store),
    CategoryIconOption("carrier", MiuixIcons.Carrier),
    CategoryIconOption("bank_cards", MiuixIcons.BankCards),
    CategoryIconOption("home", MiuixIcons.Home),
    CategoryIconOption("music", MiuixIcons.Music),
    CategoryIconOption("promotions", MiuixIcons.Promotions),
    CategoryIconOption("more", MiuixIcons.More),
    CategoryIconOption("favorites", MiuixIcons.Favorites),
    CategoryIconOption("favorites_fill", MiuixIcons.FavoritesFill),
    CategoryIconOption("contacts", MiuixIcons.Contacts),
    CategoryIconOption("community", MiuixIcons.Community),
    CategoryIconOption("phone", MiuixIcons.Phone),
    CategoryIconOption("map_album", MiuixIcons.MapAlbum),
    CategoryIconOption("send", MiuixIcons.Send),
    CategoryIconOption("import", MiuixIcons.Import),
)

/**
 * 提供分类可选图标集合，按“添加”对话框中每行五个排列，自定义彩色图标在前。
 */
internal val categoryIconOptions: List<CategoryIconOption> =
    customCategoryIconOptions + miuiCategoryIconOptions

/**
 * 返回图标键对应的图标选项，兼容旧分类名称，未匹配时回退到“其他”。
 */
internal fun categoryIconOption(
    iconKey: String,
    fallbackName: String,
): CategoryIconOption = categoryIconOptions.firstOrNull { it.key == iconKey }
    ?: categoryIconOptions.firstOrNull { it.key == defaultCategoryIconKey(fallbackName) }
    ?: categoryIconOptions.first()

/**
 * 返回分类名称对应的默认图标键，用于补全历史与默认分类。
 */
internal fun defaultCategoryIconKey(name: String): String = when (name) {
    "消费" -> "custom_consumption"
    "餐饮" -> "custom_dining"
    "其他" -> "custom_other"
    "转账" -> "custom_transfer"
    "教育" -> "custom_education"
    "购物" -> "custom_shopping"
    "人情社交" -> "custom_social"
    "娱乐" -> "custom_entertainment"
    "住房" -> "custom_housing"
    "居住" -> "custom_housing"
    "交通" -> "custom_transport"
    "红包" -> "custom_red_packet"
    "投资" -> "custom_investment"
    "通讯" -> "custom_communication"
    "医疗" -> "custom_medical"
    "旅行" -> "custom_travel"
    "借出" -> "custom_lend_out"
    "还债" -> "custom_repay"
    "美容" -> "custom_beauty"
    "亲子" -> "custom_family"
    "宠物" -> "custom_pet"
    "代付" -> "custom_pay_for"
    "退款" -> "custom_refund"
    "薪资" -> "custom_salary"
    "工资" -> "custom_salary"
    "理财" -> "custom_wealth"
    "借入" -> "custom_borrow_in"
    "收债" -> "custom_collect"
    "其他收入" -> "custom_other"
    else -> "more"
}

/**
 * 返回真实分类名称对应的分类图标。
 */
internal fun accountingCategoryIcon(name: String): ImageVector =
    categoryIconOption(iconKey = defaultCategoryIconKey(name), fallbackName = name).icon

/**
 * 返回持久化图标键对应的分类图标，并兼容旧分类名称。
 */
internal fun accountingCategoryIcon(
    iconKey: String,
    fallbackName: String,
): ImageVector = categoryIconOption(iconKey, fallbackName).icon
