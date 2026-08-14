package com.vos.accounting.hook

import com.vos.accounting.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 验证微信结构化 Hook 只生成有限且方向明确的账单字段。 */
class WechatHookParserTest {
    private val parser = WechatHookParser()

    /** XWeb 支出回调应提取金额、商户、付款方式、交易单号和支付时间。 */
    @Test
    fun parseXWebExpense() {
        val script = buildXWebScript(
            fee = "-12.34",
            merchant = "测试便利店",
            fields = listOf(
                "付款方式" to "零钱",
                "支付时间" to "2026年8月1日 13:24:58",
                "交易单号" to "wx_order_1",
            ),
        )

        val capture = parser.parseXWeb(script, WechatCachedFields())

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("12.34", capture?.amount)
        assertEquals("测试便利店", capture?.merchant)
        assertEquals("零钱", capture?.paymentMethod)
        assertEquals("wx_order_1", capture?.externalTransactionId)
        assertEquals(
            LocalDateTime.of(2026, 8, 1, 13, 24, 58)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            capture?.occurredAt,
        )
    }

    /** XWeb 正金额回调应识别为收入并允许缓存补齐付款方式。 */
    @Test
    fun parseXWebIncomeWithCachedMethod() {
        val script = buildXWebScript(
            fee = "+0.01",
            merchant = "测试付款方",
            fields = listOf("支付时间" to "2026年8月1日 13:24:58"),
        )

        val capture = parser.parseXWeb(
            script,
            WechatCachedFields(paymentMethod = "储蓄卡"),
        )

        assertEquals(TransactionType.INCOME, capture?.type)
        assertEquals("0.01", capture?.amount)
        assertEquals("储蓄卡", capture?.paymentMethod)
    }

    /** XWeb 列表响应缺少原始交易时间时不应生成账单。 */
    @Test
    fun rejectXWebWithoutOriginalTime() {
        val script = buildXWebScript(
            fee = "-1.00",
            merchant = "列表商户",
            fields = listOf("交易单号" to "list_order"),
        )

        assertNull(parser.parseXWeb(script, WechatCachedFields()))
    }

    /** XWeb 详情 DOM 应提取方向、金额、商户、支付方式、交易号和原始时间。 */
    @Test
    fun parseXWebDomDetail() {
        val dom = Json.encodeToString(
            "木梓面馆\n−9.00\n当前状态\n支付成功\n支付时间\n2026年8月1日 19:49:14\n" +
                "商品\n木梓面馆\n商户全称\n商户_樊远佩\n支付方式\n零钱\n交易单号\nwx_order_4",
        )

        val capture = parser.parseXWebDom(dom)

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("9.00", capture?.amount)
        assertEquals("木梓面馆", capture?.merchant)
        assertEquals("木梓面馆", capture?.note)
        assertEquals("零钱", capture?.paymentMethod)
        assertEquals(
            LocalDateTime.of(2026, 8, 1, 19, 49, 14)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            capture?.occurredAt,
        )
    }

    /** XWeb 商户主页内嵌详情应取金额后的对象，不把页签标题当成对象。 */
    @Test
    fun parseXWebDomMerchantAfterAmount() {
        val dom = Json.encodeToString(
            "蜜雪冰城\n等328万人喜欢\n小程序\n畅饮一杯\n服务\n会员\n交易详情\n−6.80\n蜜雪冰城\n" +
                "当前状态\n支付成功\n支付时间\n2026年06月12日 20:37:16\n商品\n蜜雪冰城928085店\n" +
                "商户全称\n蜜雪冰城股份有限公司\n支付方式\n零钱\n交易单号\nwx_order_5",
        )

        val capture = parser.parseXWebDom(dom)

        assertEquals("蜜雪冰城", capture?.merchant)
        assertEquals("蜜雪冰城928085店", capture?.note)
    }

    /** 微信转账详情的“对方已收钱”状态应生成支出账单。 */
    @Test
    fun parseXWebDomReceivedTransfer() {
        val dom = Json.encodeToString(
            "转账-转给测试用户\n−55.00\n当前状态\n对方已收钱\n转账说明\n微信转账\n" +
                "转账时间\n2026年8月12日 20:52:08\n收款时间\n2026年8月12日 20:52:18\n" +
                "支付方式\n中国银行储蓄卡(3279)\n转账单号\ntransfer_order_1",
        )

        val capture = parser.parseXWebDom(dom)

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("55.00", capture?.amount)
        assertEquals("转账-转给测试用户", capture?.merchant)
        assertEquals("中国银行储蓄卡(3279)", capture?.paymentMethod)
        assertEquals("transfer_order_1", capture?.externalTransactionId)
        assertEquals(
            LocalDateTime.of(2026, 8, 12, 20, 52, 8)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            capture?.occurredAt,
        )
    }

    /** XWeb 列表 DOM 即使混有其他卡片的详情字段也不应生成账单。 */
    @Test
    fun rejectXWebListDom() {
        val dom = Json.encodeToString(
            "4月30日 11:50\n五味佳餐馆 | 紫金店\n使用零钱支付\n¥10.00\n交易状态\n" +
                "支付成功，对方已收款\n查看账单详情\n支付时间\n2026年4月30日 11:50:00\n" +
                "交易单号\nlist_order_1",
        )

        assertNull(parser.parseXWebDom(dom))
    }

    /** XWeb 时间不在已知标签位置时仍应从结构化响应中取得。 */
    @Test
    fun parseXWebTimeOutsideKnownLabels() {
        val script = buildXWebScript(
            fee = "-1.00",
            merchant = "测试商户",
            fields = listOf("完成于" to "2026年8月1日 13:24:58"),
        )

        val capture = parser.parseXWeb(script, WechatCachedFields())

        assertEquals(
            LocalDateTime.of(2026, 8, 1, 13, 24, 58)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            capture?.occurredAt,
        )
    }

    /** WCDB 支付消息应解析 CDATA、金额、对象和外部交易号。 */
    @Test
    fun parseWcdbPaymentMessage() {
        val record = WechatMessageRecord(
            table = "message",
            type = 318767153L,
            isSend = 1,
            createTime = 1_800_000_000_000L,
            fields = mapOf(
                "content" to """
                    <appmsg>
                      <title><![CDATA[支付成功]]></title>
                      <des><![CDATA[付款金额 ￥23.45
                      商户：测试超市]]></des>
                      <transaction_id><![CDATA[wx_order_2]]></transaction_id>
                    </appmsg>
                """.trimIndent(),
            ),
        )

        val capture = parser.parseMessage(
            record,
            WechatCachedFields(paymentMethod = "信用卡"),
        )

        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals("23.45", capture?.amount)
        assertEquals("测试超市", capture?.merchant)
        assertEquals("信用卡", capture?.paymentMethod)
        assertEquals("wx_order_2", capture?.externalTransactionId)
        assertEquals(1_800_000_000_000L, capture?.occurredAt)
    }

    /** 未出现微信支付成功标记的普通 JavaScript 不应生成账单。 */
    @Test
    fun rejectUnrelatedJavascript() {
        val capture = parser.parseXWeb(
            "javascript:console.log('hello')",
            WechatCachedFields(amount = "1.00"),
        )

        assertNull(capture)
    }

    /** 构造与微信 XWeb 回调层级一致的测试脚本。 */
    private fun buildXWebScript(
        fee: String,
        merchant: String,
        fields: List<Pair<String, String>>,
    ): String {
        val response = buildJsonObject {
            put("header", buildJsonObject {
                put("fee", fee)
                put("nickname", merchant)
            })
            put("preview", buildJsonArray {
                fields.forEach { (label, value) ->
                    add(buildJsonObject {
                        put("label", buildJsonObject { put("name", label) })
                        put("value", buildJsonArray {
                            add(buildJsonObject { put("name", value) })
                        })
                    })
                }
            })
        }
        val outer = buildJsonObject {
            put("__json_message", buildJsonObject {
                put("__params", buildJsonObject {
                    put("respbuf", response.toString())
                })
            })
        }
        return "javascript:WeixinJSBridge._handleMessageFromWeixin($outer);nativeWXPayCgiTunnel:ok"
    }
}
