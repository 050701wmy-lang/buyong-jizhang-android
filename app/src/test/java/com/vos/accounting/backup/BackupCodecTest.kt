package com.vos.accounting.backup

import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountLedgerCrossRef
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.AppSettingsEntity
import com.vos.accounting.data.AutoAccountMappingEntity
import com.vos.accounting.data.AutoBookkeepingEventEntity
import com.vos.accounting.data.AutoCategoryMappingEntity
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.LedgerEntity
import com.vos.accounting.data.TransactionEntity
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.AutoBookkeepingStatus
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 验证备份加密与压缩编解码的往返一致性和密码校验。
 */
class BackupCodecTest {
    @Test
    fun encryptDecryptRoundTrip() {
        val payload = "备份内容测试".toByteArray(Charsets.UTF_8)
        val blob = BackupCodec.encrypt(payload, "pass-123")
        assertArrayEquals(payload, BackupCodec.decrypt(blob, "pass-123"))
    }

    @Test
    fun wrongPasswordFails() {
        val blob = BackupCodec.encrypt("secret".toByteArray(Charsets.UTF_8), "right")
        assertThrows(BackupException::class.java) {
            BackupCodec.decrypt(blob, "wrong")
        }
    }

    @Test
    fun zipRoundTripWithMedia() {
        val data = BackupData(
            createdAt = 1,
            media = listOf(BackupMediaRef("ledger_cover_1.jpg")),
        )
        val media = mapOf("ledger_cover_1.jpg" to byteArrayOf(1, 2, 3))
        val content = BackupCodec.unzip(BackupCodec.buildZip(data, media))
        assertEquals(1, content.data.media.size)
        assertEquals("ledger_cover_1.jpg", content.data.media.single().entryName)
        assertArrayEquals(byteArrayOf(1, 2, 3), content.media["ledger_cover_1.jpg"])
    }

    @Test
    fun jsonRoundTripPreservesEntities() {
        val data = BackupData(
            createdAt = 123,
            accounts = emptyList(),
            ledgers = emptyList(),
            accountLedgerCrossRefs = emptyList(),
            accountTypes = emptyList(),
            currencies = emptyList(),
            categories = emptyList(),
            transactions = emptyList(),
            settings = null,
            media = emptyList(),
        )
        val content = BackupCodec.unzip(BackupCodec.buildZip(data, emptyMap()))
        assertEquals(123, content.data.createdAt)
        assertEquals(0, content.data.accounts.size)
    }

    @Test
    fun invalidPayloadRejected() {
        assertThrows(BackupException::class.java) {
            BackupCodec.decrypt("not a backup".toByteArray(Charsets.UTF_8), "pw")
        }
    }

    @Test
    fun entityRoundTrip() {
        val data = BackupData(
            createdAt = 7,
            accounts = listOf(
                AccountEntity(
                    id = 2,
                    name = "美元卡",
                    type = AccountType.BANK_CARD,
                    typeKey = "bank_card",
                    currencyKey = "usd",
                    openingBalanceMinor = 100,
                    sortOrder = 1,
                    iconKey = "debit_card",
                    isDefault = false,
                    isArchived = false,
                ),
            ),
            ledgers = listOf(
                LedgerEntity(
                    id = 1,
                    name = "日常账本",
                    coverKey = "cover_ocean",
                    useLightText = true,
                    baseCurrencyKey = "cny",
                    isHidden = false,
                    sortOrder = 0,
                ),
            ),
            accountLedgerCrossRefs = listOf(AccountLedgerCrossRef(accountId = 2, ledgerId = 1)),
            accountTypes = listOf(
                AccountTypeEntity(
                    key = "bank_card",
                    name = "储蓄卡",
                    summary = "",
                    iconKey = "debit_card",
                    baseType = AccountType.BANK_CARD,
                    isBuiltin = true,
                ),
            ),
            currencies = listOf(
                CurrencyEntity(
                    key = "usd",
                    code = "USD",
                    name = "美元",
                    symbol = "$",
                    rateToCnyScaled = 675977000,
                    isBuiltin = true,
                    updatedAt = 0,
                    autoRateEnabled = true,
                ),
            ),
            categories = listOf(
                CategoryEntity(
                    id = 3,
                    name = "餐饮",
                    type = TransactionType.EXPENSE,
                    sortOrder = 0,
                    iconKey = "store",
                    isArchived = false,
                ),
            ),
            transactions = listOf(
                TransactionEntity(
                    id = 4,
                    type = TransactionType.INCOME,
                    amountMinor = 100,
                    accountId = 2,
                    categoryId = null,
                    merchant = "",
                    note = "币种兑换转入",
                    occurredAt = 5,
                    source = TransactionSource.MANUAL,
                    ledgerId = 1,
                    currencyKey = "usd",
                    baseAmountMinor = 67598,
                    baseCurrencyKey = "cny",
                    accountAmountMinor = 100,
                    exchangeId = 9,
                ),
            ),
            settings = AppSettingsEntity(
                id = 1,
                themeMode = "SYSTEM",
                followSystemColor = true,
                predictiveBackAnimationEnabled = false,
                currentLedgerId = 1,
            ),
            media = emptyList(),
        )
        val content = BackupCodec.unzip(BackupCodec.buildZip(data, emptyMap()))
        val restored = content.data
        assertEquals(2, restored.accounts.single().id)
        assertEquals("usd", restored.accounts.single().currencyKey)
        assertEquals(1, restored.ledgers.single().id)
        assertEquals(TransactionType.INCOME, restored.transactions.single().type)
        assertEquals(9L, restored.transactions.single().exchangeId)
        assertEquals(1, restored.settings?.currentLedgerId ?: 0)
        assertEquals(1, restored.accountLedgerCrossRefs.single().ledgerId)
    }

    /** 验证 v4 备份保留待确认草稿和两类本地学习映射。 */
    @Test
    fun versionFourPreservesAutoBookkeepingData() {
        val event = AutoBookkeepingEventEntity(
            id = 8,
            provider = PaymentProvider.WECHAT,
            status = AutoBookkeepingStatus.PENDING,
            type = TransactionType.EXPENSE,
            amountMinor = 1234,
            accountId = 2,
            categoryId = 3,
            merchant = "商店",
            note = "",
            occurredAt = 10,
            paymentMethodKey = "零钱",
            destinationPaymentMethodKey = "",
            externalKeyHash = "hash",
            fingerprint = "fingerprint",
            captureSources = "NOTIFICATION",
            canConfirm = true,
            ledgerId = 1,
            createdAt = 10,
            updatedAt = 10,
        )
        val data = BackupData(
            createdAt = 10,
            autoBookkeepingEvents = listOf(event),
            autoCategoryMappings = listOf(
                AutoCategoryMappingEntity(PaymentProvider.WECHAT, "商店", TransactionType.EXPENSE, 3),
            ),
            autoAccountMappings = listOf(
                AutoAccountMappingEntity(PaymentProvider.WECHAT, "零钱", 2),
            ),
        )

        val restored = BackupCodec.unzip(BackupCodec.buildZip(data, emptyMap())).data

        assertEquals(4, restored.formatVersion)
        assertEquals(event, restored.autoBookkeepingEvents.single())
        assertEquals(3, restored.autoCategoryMappings.single().categoryId)
        assertEquals(2, restored.autoAccountMappings.single().accountId)
    }
}
