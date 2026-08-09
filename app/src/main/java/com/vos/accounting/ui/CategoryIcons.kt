package com.vos.accounting.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 表示分类编辑器中的持久化 MIUIX 图标。
 */
internal data class CategoryIconOption(
    val key: String,
    val icon: ImageVector,
)

/**
 * 提供分类可选图标集合并保持已有 icon_key 兼容。
 */
internal val categoryIconOptions: List<CategoryIconOption> = listOf(
    CategoryIconOption("custom_consumption", CategoryConsumptionIcon),
    CategoryIconOption("custom_dining", CategoryDiningIcon),
    CategoryIconOption("custom_other", CategoryOtherIcon),
    CategoryIconOption("custom_transfer", CategoryTransferIcon),
    CategoryIconOption("custom_education", CategoryEducationIcon),
    CategoryIconOption("custom_shopping", CategoryShoppingIcon),
    CategoryIconOption("custom_social", CategorySocialIcon),
    CategoryIconOption("custom_entertainment", CategoryEntertainmentIcon),
    CategoryIconOption("custom_housing", CategoryHousingIcon),
    CategoryIconOption("custom_transport", CategoryTransportIcon),
    CategoryIconOption("custom_red_packet", CategoryRedPacketIcon),
    CategoryIconOption("custom_investment", CategoryInvestmentIcon),
    CategoryIconOption("custom_communication", CategoryCommunicationIcon),
    CategoryIconOption("custom_medical", CategoryMedicalIcon),
    CategoryIconOption("custom_travel", CategoryTravelIcon),
    CategoryIconOption("custom_lend_out", CategoryLendOutIcon),
    CategoryIconOption("custom_repay", CategoryRepayIcon),
    CategoryIconOption("custom_beauty", CategoryBeautyIcon),
    CategoryIconOption("custom_family", CategoryFamilyIcon),
    CategoryIconOption("custom_pet", CategoryPetIcon),
    CategoryIconOption("custom_pay_for", CategoryPayForIcon),
    CategoryIconOption("custom_refund", CategoryRefundIcon),
    CategoryIconOption("custom_salary", CategorySalaryIcon),
    CategoryIconOption("custom_wealth", CategoryWealthIcon),
    CategoryIconOption("custom_borrow_in", CategoryBorrowInIcon),
    CategoryIconOption("custom_collect", CategoryCollectIcon),
)

/**
 * 将旧版通用图标键映射到语义最接近的新图标键。
 */
private val legacyCategoryIconAliases = mapOf(
    "store" to "custom_shopping",
    "carrier" to "custom_transport",
    "bank_cards" to "custom_consumption",
    "home" to "custom_housing",
    "music" to "custom_entertainment",
    "promotions" to "custom_investment",
    "more" to "custom_other",
    "favorites" to "custom_pet",
    "favorites_fill" to "custom_pet",
    "contacts" to "custom_family",
    "community" to "custom_social",
    "phone" to "custom_communication",
    "map_album" to "custom_travel",
    "send" to "custom_lend_out",
    "import" to "custom_transfer",
)

/**
 * 使用统一尺寸与主题色展示分类 MIUIX 图标。
 */
@Composable
internal fun CategoryIcon(
    option: CategoryIconOption,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        imageVector = option.icon,
        contentDescription = null,
        modifier = modifier,
        tint = tint ?: MiuixTheme.colorScheme.primary,
    )
}

/**
 * 返回图标键对应的图标选项，兼容旧分类名称，未匹配时回退到“其他”。
 */
internal fun categoryIconOption(
    iconKey: String,
    fallbackName: String,
): CategoryIconOption = categoryIconOptions.firstOrNull {
    it.key == (legacyCategoryIconAliases[iconKey] ?: iconKey)
}
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
    "住房", "居住" -> "custom_housing"
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
    "薪资", "工资" -> "custom_salary"
    "理财" -> "custom_wealth"
    "借入" -> "custom_borrow_in"
    "收债" -> "custom_collect"
    "其他收入" -> "custom_other"
    else -> "custom_other"
}
