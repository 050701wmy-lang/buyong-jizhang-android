package com.vos.accounting.auto

import com.vos.accounting.data.AutoBookkeepingEventEntity
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionSource

/**
 * 将待确认的自动账单转换为可由用户继续编辑的交易草稿。
 */
internal fun AutoBookkeepingEventEntity.toEditableTransactionDraft(): TransactionDraft =
    TransactionDraft(
        type = type,
        amountMinor = amountMinor,
        currencyKey = currencyKey,
        accountAmountMinor = amountMinor,
        accountId = accountId ?: 0L,
        categoryId = categoryId,
        merchant = merchant,
        note = note,
        occurredAt = occurredAt,
        source = TransactionSource.AI,
        ledgerId = ledgerId,
    )
