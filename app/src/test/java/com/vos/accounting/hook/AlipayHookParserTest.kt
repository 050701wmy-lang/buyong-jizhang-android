package com.vos.accounting.hook

import com.vos.accounting.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 验证支付宝同步消息与账单 DOM 的本地解析规则。 */
class AlipayHookParserTest {
    private val parser = AlipayHookParser()

    /** 验证标准成功交易同步消息可提取全部关键字段。 */
    @Test
    fun parsesExpenseSyncMessage() {
        val capture = parser.parseSync(
            """
            [{
              "tradeStatus": "TRADE_SUCCESS",
              "totalAmount": "12.34",
              "merchantName": "测试商户",
              "subject": "测试商品",
              "fundChannel": "余额宝",
              "tradeNo": "202608112200001",
              "gmtPayment": "2026-08-11 20:30:00"
            }]
            """.trimIndent(),
        )

        assertNotNull(capture)
        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("12.34", capture?.amount)
        assertEquals("测试商户", capture?.merchant)
        assertEquals("测试商品", capture?.note)
        assertEquals("余额宝", capture?.paymentMethod)
        assertEquals("202608112200001", capture?.externalTransactionId)
    }

    /** 验证嵌套字符串 JSON 中的收款消息可识别为收入。 */
    @Test
    fun parsesNestedIncomeSyncMessage() {
        val capture = parser.parseSync(
            """
            {
              "payload": "{\"status\":\"收款到账\",\"amount\":\"25.00\",\"merchantName\":\"张三\",\"tradeNo\":\"income-1\"}"
            }
            """.trimIndent(),
        )

        assertEquals(TransactionType.INCOME, capture?.type)
        assertEquals("25.00", capture?.amount)
        assertEquals("张三", capture?.merchant)
    }

    /** 验证普通服务消息不会被当作交易。 */
    @Test
    fun rejectsNonTransactionSyncMessage() {
        assertNull(parser.parseSync("""[{"title":"会员活动","content":"签到领取积分"}]"""))
    }

    /** 验证带交易号的详情 DOM 可提取支出字段。 */
    @Test
    fun parsesExpenseBillDom() {
        val capture = parser.parseDom(
            "\"交易成功\\n￥6.80\\n收款方\\n蜜雪冰城\\n商品说明\\n蜜雪冰城928085店" +
                "\\n付款方式\\n余额宝\\n付款时间\\n2026-08-11 20:37:16" +
                "\\n支付宝交易号\\n202608112200002\"",
        )

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("6.80", capture?.amount)
        assertEquals("蜜雪冰城", capture?.merchant)
        assertEquals("蜜雪冰城928085店", capture?.note)
        assertEquals("余额宝", capture?.paymentMethod)
        assertEquals("202608112200002", capture?.externalTransactionId)
    }

    /** 验证 XRiver 账单详情使用顶部商户和交易详情标识。 */
    @Test
    fun parsesXriverBillDom() {
        val capture = parser.parseDom(
            "\"万红羊蛙蛙（商贸西门店）\\n支出9.51元\\n交易成功\\n订单金额\\n10.00" +
                "\\n支付时间\\n2026-08-08 18:54:59\\n付款方式\\n花呗" +
                "\\n收款方全称\\n田家庵区万红羊蛙蛙二部火锅馆（个体工商户）" +
                "\\n交易详情\\n673dd3be-e3b0-4de9-8f4f-e64a2e29c0a0\"",
        )

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("9.51", capture?.amount)
        assertEquals("万红羊蛙蛙（商贸西门店）", capture?.merchant)
        assertEquals("花呗", capture?.paymentMethod)
        assertEquals("673dd3be-e3b0-4de9-8f4f-e64a2e29c0a0", capture?.externalTransactionId)
    }

    /** 验证当前支付宝账单详情的可见文本可提取完整支出字段。 */
    @Test
    fun parsesCurrentVisibleXriverBillDom() {
        val capture = parser.parseDom(
            "\"飞宇6店\\n支出50元\\n交易成功\\n支付时间\\n2026-08-12 18:48:33" +
                "\\n付款方式\\n花呗\\n商品说明\\n飞宇网咖-陈洞路店-扫码支付" +
                "\\n收单机构\\n上海盛付通电子支付服务有限公司\"",
        )

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("50.00", capture?.amount)
        assertEquals("飞宇6店", capture?.merchant)
        assertEquals("飞宇网咖-陈洞路店-扫码支付", capture?.note)
        assertEquals("花呗", capture?.paymentMethod)
        assertEquals(
            LocalDateTime.of(2026, 8, 12, 18, 48, 33)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            capture?.occurredAt,
        )
    }

    /** 验证折叠交易号的收益详情仍可生成收入候选。 */
    @Test
    fun parsesIncomeBillDomWithoutExternalId() {
        val capture = parser.parseDom(
            "\"测试货币基金\\n￥0.41\\n交易成功\\n创建时间\\n2026-08-13 06:17:23" +
                "\\n商品说明\\n余额宝-2026.08.12-收益发放\\n对方账户\\n测试货币基金\"",
        )

        assertEquals(TransactionType.INCOME, capture?.type)
        assertEquals("0.41", capture?.amount)
        assertEquals("测试货币基金", capture?.merchant)
        assertNull(capture?.externalTransactionId)
    }
}
