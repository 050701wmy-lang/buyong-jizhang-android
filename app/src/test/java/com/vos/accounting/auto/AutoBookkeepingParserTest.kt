package com.vos.accounting.auto

import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 验证三个支付平台脱敏文本夹具的提取与拒绝规则。 */
class AutoBookkeepingParserTest {
    /** 验证微信付款结果页可提取金额、商户、账户与订单摘要。 */
    @Test
    fun parsesWechatExpensePage() {
        val capture = parseAutoBookkeepingText(
            packageName = WECHAT_PACKAGE,
            textParts = listOf(
                "支付成功",
                "￥58.20",
                "商户：示例咖啡店",
                "付款方式：零钱",
                "交易单号：420000000000000000",
            ),
            source = AutoCaptureSource.ACCESSIBILITY,
            occurredAt = 1000,
        )

        assertNotNull(capture)
        assertEquals(PaymentProvider.WECHAT, capture?.provider)
        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals(5820L, capture?.amountMinor)
        assertEquals("示例咖啡店", capture?.merchant)
        assertEquals("零钱", capture?.paymentMethodKey)
        assertFalse(capture?.externalKeyHash.orEmpty().contains("420000"))
    }

    /** 验证支付宝收款通知可识别收入。 */
    @Test
    fun parsesAlipayIncomeNotification() {
        val capture = parseAutoBookkeepingText(
            packageName = ALIPAY_PACKAGE,
            textParts = listOf("收款成功", "收到 20.00 元", "收款方", "个人收款码"),
            source = AutoCaptureSource.NOTIFICATION,
            occurredAt = 2000,
        )

        assertEquals(TransactionType.INCOME, capture?.type)
        assertEquals(2000L, capture?.amountMinor)
        assertEquals("个人收款码", capture?.merchant)
    }

    /** 验证支付宝账单详情的“交易成功”文案可与付款方式共同确认支出。 */
    @Test
    fun parsesAlipaySuccessfulTransactionPage() {
        val capture = parseAutoBookkeepingText(
            packageName = ALIPAY_PACKAGE,
            textParts = listOf("交易成功", "12.34元", "付款方式", "余额"),
            source = AutoCaptureSource.ACCESSIBILITY,
            occurredAt = 100L,
        )

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals(1234L, capture?.amountMinor)
        assertEquals("余额", capture?.paymentMethodKey)
    }

    /** 验证云闪付退款通知生成收入草稿。 */
    @Test
    fun parsesUnionPayRefundNotification() {
        val capture = parseAutoBookkeepingText(
            packageName = UNIONPAY_PACKAGE,
            textParts = listOf("退款到账", "人民币 12.34", "商户 商店甲", "到账账户 尾号1234"),
            source = AutoCaptureSource.NOTIFICATION,
            occurredAt = 3000,
        )

        assertEquals(TransactionType.INCOME, capture?.type)
        assertEquals(1234L, capture?.amountMinor)
        assertEquals("商店甲", capture?.merchant)
    }

    /** 验证转账结果按普通支出提取来源金额和账户文本。 */
    @Test
    fun parsesTransferAmounts() {
        val capture = parseAutoBookkeepingText(
            packageName = ALIPAY_PACKAGE,
            textParts = listOf(
                "转账成功",
                "转出金额：¥100.00",
                "转入金额：¥13.75",
                "转出账户：人民币余额",
                "转入账户：美元余额",
            ),
            source = AutoCaptureSource.ACCESSIBILITY,
            occurredAt = 4000,
        )

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals(10_000L, capture?.amountMinor)
        assertEquals("人民币余额", capture?.paymentMethodKey)
    }

    /** 验证无支付结果语义、字段缺失和非白名单包名都会被拒绝。 */
    @Test
    fun rejectsUncertainOrUnsupportedText() {
        assertNull(
            parseAutoBookkeepingText(
                WECHAT_PACKAGE,
                listOf("订单详情", "￥18.00"),
                AutoCaptureSource.ACCESSIBILITY,
                1,
            ),
        )
        assertNull(
            parseAutoBookkeepingText(
                ALIPAY_PACKAGE,
                listOf("支付成功", "金额暂不可用"),
                AutoCaptureSource.ACCESSIBILITY,
                1,
            ),
        )
        assertNull(
            parseAutoBookkeepingText(
                "com.example.payment",
                listOf("支付成功", "¥18.00"),
                AutoCaptureSource.NOTIFICATION,
                1,
            ),
        )
    }
}
