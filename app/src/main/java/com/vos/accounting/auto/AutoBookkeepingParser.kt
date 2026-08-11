package com.vos.accounting.auto

import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.MAX_AMOUNT_MINOR
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import java.math.BigDecimal
import java.security.MessageDigest

/** 微信 Android 客户端包名。 */
const val WECHAT_PACKAGE = "com.tencent.mm"

/** 支付宝 Android 客户端包名。 */
const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"

/** 云闪付 Android 客户端包名。 */
const val UNIONPAY_PACKAGE = "com.unionpay"

private val supportedPackages = setOf(WECHAT_PACKAGE, ALIPAY_PACKAGE, UNIONPAY_PACKAGE)
private val amountPatterns = listOf(
    Regex("(?:￥|¥|人民币|CNY\\s*)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
    Regex("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*元"),
)
private val merchantLabels = listOf("商户", "收款方", "付款给", "收款人", "交易对象", "商品")
private val paymentMethodLabels = listOf("付款方式", "支付方式", "扣款方式", "付款账户", "转出账户")
private val externalKeyLabels = listOf("交易单号", "订单号", "商户单号", "交易流水号")

/** 把受支持的支付客户端包名映射为平台。 */
fun paymentProviderForPackage(packageName: String): PaymentProvider? = when (packageName) {
    WECHAT_PACKAGE -> PaymentProvider.WECHAT
    ALIPAY_PACKAGE -> PaymentProvider.ALIPAY
    UNIONPAY_PACKAGE -> PaymentProvider.UNIONPAY
    else -> null
}

/** 判断包名是否属于首版允许读取的三个支付客户端。 */
fun isSupportedPaymentPackage(packageName: String): Boolean = packageName in supportedPackages

/**
 * 从支付页面节点或账单通知的平铺文本中提取最小化账单字段。
 * 未同时出现明确结果语义和合法金额时拒绝识别。
 */
fun parseAutoBookkeepingText(
    packageName: String,
    textParts: List<String>,
    source: AutoCaptureSource,
    occurredAt: Long,
): AutoBookkeepingCapture? {
    val provider = paymentProviderForPackage(packageName) ?: return null
    val lines = textParts.map(String::trim).filter(String::isNotEmpty).distinct()
    if (lines.isEmpty()) return null
    val text = lines.joinToString(" ")
    val type = detectTransactionType(text, source) ?: return null
    val sourceAmount = findAmount(lines) ?: return null
    val merchant = findLabeledValue(lines, merchantLabels).orEmpty()
    val paymentMethod = findLabeledValue(lines, paymentMethodLabels).orEmpty()
    val externalKey = findLabeledValue(lines, externalKeyLabels)
    return AutoBookkeepingCapture(
        provider = provider,
        source = source,
        type = type,
        amountMinor = sourceAmount,
        merchant = merchant,
        occurredAt = occurredAt,
        paymentMethodKey = paymentMethod,
        externalKeyHash = externalKey?.let { value -> hashExternalKey(provider, value) },
    )
}

/** 从结果文案中判断支出或收入，退款归为收入，转账归为支出。 */
private fun detectTransactionType(text: String, source: AutoCaptureSource): TransactionType? = when {
    listOf("退款成功", "退款到账", "已退款", "退款完成").any(text::contains) -> TransactionType.INCOME
    listOf("收款成功", "已收款", "收款到账", "收款通知").any(text::contains) -> TransactionType.INCOME
    listOf("转账成功", "转账完成", "转账已到账").any(text::contains) -> TransactionType.EXPENSE
    listOf("支付成功", "付款成功", "已支付", "成功付款", "扣款成功", "消费成功").any(text::contains) -> {
        TransactionType.EXPENSE
    }
    text.contains("交易成功") && paymentMethodLabels.any(text::contains) -> TransactionType.EXPENSE
    source == AutoCaptureSource.NOTIFICATION && listOf("退款", "退回").any(text::contains) -> {
        TransactionType.INCOME
    }
    source == AutoCaptureSource.NOTIFICATION && listOf("收款", "收入").any(text::contains) -> {
        TransactionType.INCOME
    }
    source == AutoCaptureSource.NOTIFICATION && listOf("支出", "消费扣款").any(text::contains) -> {
        TransactionType.EXPENSE
    }
    else -> null
}

/** 返回文本中第一个合法人民币金额。 */
private fun findAmount(lines: List<String>): Long? = lines.firstNotNullOfOrNull { line ->
    amountPatterns.firstNotNullOfOrNull { pattern ->
        pattern.find(line)?.groupValues?.getOrNull(1)?.let(::parseAmountMinor)
    }
}

/** 把十进制元金额安全转换为最小货币单位。 */
private fun parseAmountMinor(value: String): Long? = runCatching {
    BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact()
}.getOrNull()?.takeIf { it in 1..MAX_AMOUNT_MINOR }

/** 读取“字段名: 值”、同节点拼接或相邻节点形式的字段。 */
private fun findLabeledValue(lines: List<String>, labels: List<String>): String? {
    lines.forEachIndexed { index, line ->
        labels.forEach { label ->
            if (line == label) return lines.getOrNull(index + 1)?.takeIf(String::isNotBlank)
            if (line.startsWith(label)) {
                val value = line.removePrefix(label).trimStart('：', ':', ' ', '\t')
                if (value.isNotEmpty()) return value
            }
        }
    }
    return null
}

/** 将外部交易标识与平台一起散列，避免保存可还原的订单号。 */
private fun hashExternalKey(provider: PaymentProvider, value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("${provider.name}|${value.trim()}".toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
