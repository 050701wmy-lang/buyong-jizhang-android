package com.vos.accounting.auto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证私有 AI 请求文本的脱敏与体积边界。 */
class PrivateAutoBookkeepingAiClientTest {
    /** 验证手机号、订单号和其他长数字不会进入请求。 */
    @Test
    fun redactsSensitiveIdentifiers() {
        val redacted = redactAiText(
            "支付成功 12.34 元 手机 13812345678 订单号：2026081100012345 卡尾号 6222021234567890",
        )

        assertFalse(redacted.contains("13812345678"))
        assertFalse(redacted.contains("2026081100012345"))
        assertFalse(redacted.contains("6222021234567890"))
        assertTrue(redacted.contains("12.34"))
    }

    /** 验证发送文本在 UTF-8 最坏情况下仍不超过 8KiB。 */
    @Test
    fun limitsRedactedTextToEightKiB() {
        val redacted = redactAiText("账".repeat(20_000))

        assertTrue(redacted.toByteArray(Charsets.UTF_8).size <= 8 * 1024)
    }
}
