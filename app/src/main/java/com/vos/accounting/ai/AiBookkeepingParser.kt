package com.vos.accounting.ai

import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 解析自然语言并生成必须经用户确认的记账草稿。
 */
class AiBookkeepingParser {
    /**
     * 从一句自然语言中提取金额、方向、分类与备注。
     */
    fun parse(
        text: String,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        occurredAt: Long,
    ): TransactionDraft {
        val amountText = AMOUNT_REGEX.find(text)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("没有识别到金额")
        val amountMinor = BigDecimal(amountText)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        val type = if (INCOME_WORDS.any(text::contains)) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
        val matchingCategories = categories.filter { it.type == type }
        val matchedCategory = matchingCategories
            .firstOrNull { text.contains(it.name) }
            ?: matchingCategories.firstOrNull { category ->
                CATEGORY_KEYWORDS[category.name]?.any(text::contains) == true
            }
            ?: matchingCategories.first()

        return TransactionDraft(
            type = type,
            amountMinor = amountMinor,
            accountId = accounts.first().id,
            categoryId = matchedCategory.id,
            merchant = extractMerchant(text, matchedCategory.name, amountText),
            note = text.trim(),
            occurredAt = occurredAt,
            source = TransactionSource.AI,
        )
    }

    /**
     * 从原句中移除金额和分类，得到简短商户描述。
     */
    private fun extractMerchant(
        text: String,
        categoryName: String,
        amountText: String,
    ): String = text
        .replace(amountText, "")
        .replace(categoryName, "")
        .replace(UNIT_REGEX, "")
        .replace(ACTION_REGEX, "")
        .trim()
        .ifEmpty { "智能记账" }

    companion object {
        private val AMOUNT_REGEX = Regex("""(\d+(?:\.\d{1,2})?)""")
        private val UNIT_REGEX = Regex("""[元块￥¥]""")
        private val ACTION_REGEX = Regex("""(花了|花费|消费|支付|买了|收入|收到|到账|赚了)""")
        private val INCOME_WORDS = listOf("收入", "收到", "到账", "工资", "奖金", "赚了")
        private val CATEGORY_KEYWORDS = mapOf(
            "餐饮" to listOf("饭", "餐", "咖啡", "奶茶", "外卖", "早餐", "午餐", "晚餐"),
            "交通" to listOf("打车", "地铁", "公交", "加油", "停车", "机票", "火车"),
            "购物" to listOf("买", "超市", "商场", "网购", "淘宝", "京东"),
            "居住" to listOf("房租", "水费", "电费", "燃气", "物业"),
            "娱乐" to listOf("电影", "游戏", "演出", "会员"),
            "工资" to listOf("工资", "薪资"),
            "奖金" to listOf("奖金", "红包"),
        )
    }
}
