package com.vos.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** 验证币种兑换后的账户详情仍能追溯完整历史账户链。 */
class AccountLineageTest {
    /** 连续两次兑换时包含所有前身账户，并排除无关兑换链。 */
    @Test
    fun linked_account_ids_follow_exchange_chain() {
        val result = linkedAccountIds(
            accountId = 3L,
            exchangeAccountIds = listOf(setOf(1L, 2L), setOf(2L, 3L), setOf(4L, 5L)),
        )

        assertEquals(setOf(1L, 2L, 3L), result)
    }
}
