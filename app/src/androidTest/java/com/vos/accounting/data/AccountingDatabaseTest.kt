package com.vos.accounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionDraft
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import com.vos.accounting.model.TransferDirection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证真实 Room DAO 与 Repository 的关键写入不变量。
 */
@RunWith(AndroidJUnit4::class)
class AccountingDatabaseTest {
    private lateinit var database: AccountingDatabase
    private lateinit var dao: AccountingDao
    private lateinit var repository: AccountingRepository

    /**
     * 为每个测试创建独立的内存数据库。
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AccountingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.accountingDao()
        repository = AccountingRepository(dao)
    }

    /**
     * 在测试结束后关闭数据库。
     */
    @After
    fun tearDown() {
        database.close()
    }

    /**
     * 验证切换和停用默认账户后仍保留唯一有效默认项。
     */
    @Test
    fun defaultAccountRemainsUniqueAfterSaveAndArchive() = runBlocking {
        repository.initialize()
        val bankId = repository.saveAccount(
            AccountEntity(
                name = "银行卡",
                type = AccountType.BANK_CARD,
                typeKey = "bank_card",
                currencyKey = "cny",
                openingBalanceMinor = 0,
                sortOrder = 0,
                isDefault = true,
            ),
            setOf(1),
        )

        assertEquals(listOf(bankId), dao.observeAccounts().first().filter(AccountEntity::isDefault).map(AccountEntity::id))

        repository.archiveAccount(bankId)

        val activeDefaults = dao.observeAccounts().first().filter {
            it.isDefault && !it.isArchived
        }
        assertEquals(1, activeDefaults.size)
        assertEquals("现金", activeDefaults.single().name)
    }

    /**
     * 验证干净安装时先建立币种再建立默认账本，外键约束下初始数据完整。
     */
    @Test
    fun cleanInstallSeedsLedgerAndCurrencies() = runBlocking {
        repository.initialize()

        val currencyKeys = dao.observeCurrencies().first().map(CurrencyEntity::key)
        assertTrue("cny" in currencyKeys)

        val ledger = dao.observeLedgers().first().single()
        assertEquals("cny", ledger.baseCurrencyKey)

        assertEquals(1, dao.observeAccounts().first().size)
        val crossRefs = dao.observeAccountLedgerCrossRefs().first()
        assertEquals(setOf(1L), crossRefs.map { it.accountId }.toSet())
        assertEquals(setOf(1L), crossRefs.map { it.ledgerId }.toSet())
    }

    /**
     * 验证保存流程拒绝账本不存在的账目。
     */
    @Test
    fun repositoryRejectsTransactionWithMissingLedger() = runBlocking {
        repository.initialize()
        val account = dao.observeAccounts().first().single()
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }

        val error = assertThrows(AccountingWriteException::class.java) {
            runBlocking {
                repository.saveTransaction(
                    TransactionDraft(
                        type = TransactionType.EXPENSE,
                        amountMinor = 100,
                        accountId = account.id,
                        categoryId = expenseCategory.id,
                        merchant = "",
                        note = "",
                        occurredAt = 1,
                        source = TransactionSource.MANUAL,
                        ledgerId = 999,
                    ),
                )
            }
        }

        assertEquals("所选账本不存在", error.message)
    }

    /**
     * 验证保存流程拒绝不属于账本的账户。
     */
    @Test
    fun repositoryRejectsAccountNotInLedger() = runBlocking {
        repository.initialize()
        val account = dao.observeAccounts().first().single()
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }
        dao.deleteAccountLedgerCrossRefs(account.id)

        val error = assertThrows(AccountingWriteException::class.java) {
            runBlocking {
                repository.saveTransaction(
                    TransactionDraft(
                        type = TransactionType.EXPENSE,
                        amountMinor = 100,
                        accountId = account.id,
                        categoryId = expenseCategory.id,
                        merchant = "",
                        note = "",
                        occurredAt = 1,
                        source = TransactionSource.MANUAL,
                    ),
                )
            }
        }

        assertEquals("所选账户不属于当前账本", error.message)
    }

    /**
     * 验证编辑历史账目时允许保留原账户与账本的组合。
     */
    @Test
    fun repositoryKeepsOriginalLedgerLinkWhenEditing() = runBlocking {
        repository.initialize()
        val account = dao.observeAccounts().first().single()
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }
        val id = repository.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 100,
                accountId = account.id,
                categoryId = expenseCategory.id,
                merchant = "",
                note = "",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
            ),
        )
        dao.deleteAccountLedgerCrossRefs(account.id)

        repository.updateTransaction(
            id,
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 200,
                accountId = account.id,
                categoryId = expenseCategory.id,
                merchant = "",
                note = "",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
            ),
        )

        assertEquals(200L, dao.findTransaction(id)?.amountMinor)
    }

    /**
     * 验证保存账目时固化币种与本位币金额快照，历史报表不随后续汇率变化。
     */
    @Test
    fun saveTransactionFreezesBaseAmountSnapshot() = runBlocking {
        repository.initialize()
        val usdAccountId = repository.saveAccount(
            AccountEntity(
                name = "美元卡",
                type = AccountType.BANK_CARD,
                typeKey = "bank_card",
                currencyKey = "usd",
                openingBalanceMinor = 0,
                sortOrder = 0,
                isDefault = false,
            ),
            setOf(1),
        )
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }
        val id = repository.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 10000,
                accountId = usdAccountId,
                categoryId = expenseCategory.id,
                merchant = "",
                note = "",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
            ),
        )

        val stored = dao.findTransaction(id)
        val usdRate = dao.findCurrency("usd")!!.rateToCnyScaled
        val cnyRate = dao.findCurrency("cny")!!.rateToCnyScaled
        assertEquals("usd", stored?.currencyKey)
        assertEquals(convertCurrencyMinor(10000, usdRate, cnyRate), stored?.baseAmountMinor)

        dao.updateBuiltinCurrencyRate("USD", 800000000, System.currentTimeMillis())
        val after = dao.findTransaction(id)
        assertEquals(stored?.baseAmountMinor, after?.baseAmountMinor)
    }

    /**
     * 验证收支汇总按固化本位币金额统计。
     */
    @Test
    fun overviewTotalsUseBaseAmountSnapshot() = runBlocking {
        repository.initialize()
        val usdAccountId = repository.saveAccount(
            AccountEntity(
                name = "美元卡",
                type = AccountType.BANK_CARD,
                typeKey = "bank_card",
                currencyKey = "usd",
                openingBalanceMinor = 0,
                sortOrder = 0,
                isDefault = false,
            ),
            setOf(1),
        )
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }
        repository.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 10000,
                accountId = usdAccountId,
                categoryId = expenseCategory.id,
                merchant = "",
                note = "",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
            ),
        )

        val usdRate = dao.findCurrency("usd")!!.rateToCnyScaled
        val cnyRate = dao.findCurrency("cny")!!.rateToCnyScaled
        val totals = dao.observeOverviewTotals().first()
        assertEquals(convertCurrencyMinor(10000, usdRate, cnyRate), totals.expenseMinor)
        assertEquals(0, totals.incomeMinor)
    }

    /**
     * 验证币种仍被历史账目使用时拒绝删除。
     */
    @Test
    fun deleteCurrencyRejectsTransactions() = runBlocking {
        repository.initialize()
        val currencyKey = repository.addCurrency("测试币", "T$", 50000000)
        val account = dao.observeAccounts().first().single()
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }
        dao.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amountMinor = 100,
                accountId = account.id,
                categoryId = expenseCategory.id,
                merchant = "",
                note = "",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
                ledgerId = 1,
                currencyKey = currencyKey,
                baseAmountMinor = 50,
            ),
        )

        val error = assertThrows(AccountingWriteException::class.java) {
            runBlocking { repository.deleteCurrency(currencyKey) }
        }
        assertEquals("该币种仍被历史账目使用，无法删除", error.message)
    }

    /**
     * 验证非空账户换币走审计式兑换：创建继任账户、成对转账流水并归档原账户。
     */
    @Test
    fun changeCurrencyOnNonEmptyAccountCreatesExchange() = runBlocking {
        repository.initialize()
        val oldId = repository.saveAccount(
            AccountEntity(
                name = "人民币卡",
                type = AccountType.BANK_CARD,
                typeKey = "bank_card",
                currencyKey = "cny",
                openingBalanceMinor = 10000,
                sortOrder = 0,
                isDefault = true,
            ),
            setOf(1),
        )

        val newId = repository.saveAccount(
            AccountEntity(
                name = "美元卡",
                type = AccountType.BANK_CARD,
                typeKey = "bank_card",
                currencyKey = "usd",
                openingBalanceMinor = 0,
                sortOrder = 0,
                isDefault = true,
            ).copy(id = oldId),
            setOf(1),
        )

        val oldAccount = dao.findAccount(oldId)
        val newAccount = dao.findAccount(newId)
        assertEquals(true, oldAccount?.isArchived == true)
        assertEquals("usd", newAccount?.currencyKey)
        assertEquals(false, newAccount?.isArchived == true)

        val oldLegs = dao.transactionsByAccount(oldId)
        val newLegs = dao.transactionsByAccount(newId)
        assertEquals(1, oldLegs.size)
        assertEquals(1, newLegs.size)
        assertEquals(TransactionType.TRANSFER, oldLegs.single().type)
        assertEquals(TransferDirection.OUT, oldLegs.single().transferDirection)
        assertEquals(10000, oldLegs.single().amountMinor)
        assertEquals(TransactionType.TRANSFER, newLegs.single().type)
        assertEquals(TransferDirection.IN, newLegs.single().transferDirection)
        assertEquals(oldLegs.single().exchangeId, newLegs.single().exchangeId)

        val cnyRate = dao.findCurrency("cny")!!.rateToCnyScaled
        val usdRate = dao.findCurrency("usd")!!.rateToCnyScaled
        assertEquals(convertCurrencyMinor(10000, cnyRate, usdRate), newLegs.single().amountMinor)

        suspend fun balanceOf(accountId: Long, opening: Long): Long = opening + dao.transactionsByAccount(accountId).sumOf {
            when {
                it.type == TransactionType.INCOME -> it.amountMinor
                it.type == TransactionType.TRANSFER && it.transferDirection == TransferDirection.IN -> it.amountMinor
                else -> -it.amountMinor
            }
        }
        assertEquals(0, balanceOf(oldId, oldAccount!!.openingBalanceMinor))
        assertEquals(
            convertCurrencyMinor(10000, cnyRate, usdRate),
            balanceOf(newId, newAccount!!.openingBalanceMinor),
        )

        // 兑换流水不计入普通收支汇总
        val totals = dao.observeOverviewTotals().first()
        assertEquals(0, totals.incomeMinor)
        assertEquals(0, totals.expenseMinor)
    }

    /**
     * 验证空账户换币直接更新币种，不创建继任账户与转账流水。
     */
    @Test
    fun changeCurrencyOnEmptyAccountUpdatesDirectly() = runBlocking {
        repository.initialize()
        val id = repository.saveAccount(
            AccountEntity(
                name = "空账户",
                type = AccountType.CASH,
                typeKey = "cash",
                currencyKey = "cny",
                openingBalanceMinor = 0,
                sortOrder = 0,
                isDefault = false,
            ),
            setOf(1),
        )
        val updatedId = repository.saveAccount(
            AccountEntity(
                name = "空账户",
                type = AccountType.CASH,
                typeKey = "cash",
                currencyKey = "usd",
                openingBalanceMinor = 0,
                sortOrder = 0,
                isDefault = false,
            ).copy(id = id),
            setOf(1),
        )

        assertEquals(id, updatedId)
        val account = dao.findAccount(id)
        assertEquals("usd", account?.currencyKey)
        assertEquals(false, account?.isArchived == true)
        assertEquals(0, dao.transactionsByAccount(id).size)
    }

    /**
     * 验证自定义账户类型会持久化名称和可选说明。
     */
    @Test
    fun customAccountTypeIsPersisted() = runBlocking {
        repository.initialize()

        val typeKey = repository.addAccountType("礼品卡", " 商场礼品余额 ")
        val accountType = dao.findAccountType(typeKey)

        assertEquals("礼品卡", accountType?.name)
        assertEquals("商场礼品余额", accountType?.summary)
        assertEquals(AccountType.VIRTUAL, accountType?.baseType)

        repository.updateAccountType(typeKey, "商超卡", "购物余额")
        assertEquals("商超卡", dao.findAccountType(typeKey)?.name)
        assertEquals("购物余额", dao.findAccountType(typeKey)?.summary)

        repository.deleteAccountType(typeKey)
        assertEquals(null, dao.findAccountType(typeKey))
    }

    /**
     * 验证保存流程拒绝与收支方向不一致的分类。
     */
    @Test
    fun repositoryRejectsCategoryWithDifferentType() = runBlocking {
        repository.initialize()
        val account = dao.observeAccounts().first().single()
        val incomeCategory = dao.observeCategories().first().first {
            it.type == TransactionType.INCOME
        }

        val error = assertThrows(AccountingWriteException::class.java) {
            runBlocking {
                repository.saveTransaction(
                    TransactionDraft(
                        type = TransactionType.EXPENSE,
                        amountMinor = 100,
                        accountId = account.id,
                        categoryId = incomeCategory.id,
                        merchant = "",
                        note = "",
                        occurredAt = 1,
                        source = TransactionSource.MANUAL,
                    ),
                )
            }
        }

        assertEquals("分类与收支类型不一致", error.message)
    }

    /**
     * 验证商家与备注在统一保存流程中会去除首尾空白。
     */
    @Test
    fun repositoryTrimsMerchantAndNote() = runBlocking {
        repository.initialize()
        val account = dao.observeAccounts().first().single()
        val expenseCategory = dao.observeCategories().first().first {
            it.type == TransactionType.EXPENSE
        }

        repository.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 1234,
                accountId = account.id,
                categoryId = expenseCategory.id,
                merchant = "  商店  ",
                note = "  午餐  ",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
            ),
        )

        val record = dao.observeTransactions().first().single()
        assertEquals("商店", record.merchant)
        assertEquals("午餐", record.note)
    }
}
