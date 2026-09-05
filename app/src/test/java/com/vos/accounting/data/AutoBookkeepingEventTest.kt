package com.vos.accounting.data

import com.vos.accounting.model.AutoBookkeepingStatus
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证待确认账单的一键入账完整性判定。 */
class AutoBookkeepingEventTest {
    /** 字段完整的普通草稿无需依赖历史精确映射即可显示确认入口。 */
    @Test
    fun completeDraftIsReadyWithoutLegacyConfidenceFlag() {
        val event = AutoBookkeepingEventEntity(
            provider = PaymentProvider.ALIPAY,
            status = AutoBookkeepingStatus.PENDING,
            type = TransactionType.INCOME,
            amountMinor = 76,
            accountId = 1,
            categoryId = 2,
            merchant = "万家日日薪货币A类",
            note = "余额宝收益发放",
            occurredAt = 1,
            paymentMethodKey = "余额宝",
            destinationPaymentMethodKey = "",
            fingerprint = "complete_draft",
            captureSources = "XPOSED",
            canConfirm = false,
            ledgerId = 1,
            createdAt = 1,
            updatedAt = 1,
        )

        assertTrue(event.isReadyToConfirm(allowAiOneTapConfirm = false))
        assertFalse(event.copy(hasConflict = true).isReadyToConfirm(allowAiOneTapConfirm = false))
        assertFalse(event.copy(aiAssisted = true).isReadyToConfirm(allowAiOneTapConfirm = false))
        assertTrue(event.copy(aiAssisted = true).isReadyToConfirm(allowAiOneTapConfirm = true))
    }
}
