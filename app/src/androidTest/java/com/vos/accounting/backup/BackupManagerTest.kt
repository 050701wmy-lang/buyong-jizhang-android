package com.vos.accounting.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountingDao
import com.vos.accounting.data.AccountingDatabase
import com.vos.accounting.data.TransactionEntity
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 验证备份导出、解析校验与覆盖式恢复的往返一致性。
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private lateinit var database: AccountingDatabase
    private lateinit var dao: AccountingDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AccountingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.accountingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun sourceDatabase(): Pair<AccountingDatabase, AccountingDao> {
        val sourceDb = Room.inMemoryDatabaseBuilder(context, AccountingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sourceDao = sourceDb.accountingDao()
        sourceDao.seedDefaults()
        return sourceDb to sourceDao
    }

    @Test
    fun exportParseApplyRoundTrip() = runBlocking {
        val (sourceDb, sourceDao) = sourceDatabase()
        val account = sourceDao.getAllAccounts().single()
        val category = sourceDao.getAllCategories().first()
        sourceDao.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amountMinor = 100,
                accountId = account.id,
                categoryId = category.id,
                merchant = "商店",
                note = "午餐",
                occurredAt = 1,
                source = TransactionSource.MANUAL,
                ledgerId = 1,
                currencyKey = "cny",
                baseAmountMinor = 100,
            ),
        )

        val prepared = BackupManager(context, sourceDao).parse(
            BackupManager(context, sourceDao).export("pw"),
            "pw",
        )
        assertEquals(1, prepared.summary.transactionCount)
        assertEquals(1, prepared.summary.ledgerCount)
        assertEquals(1, prepared.summary.accountCount)

        BackupManager(context, dao).apply(prepared)
        assertEquals(sourceDao.getAllAccounts().map { it.id }, dao.getAllAccounts().map { it.id })
        assertEquals(sourceDao.getAllLedgers().map { it.id }, dao.getAllLedgers().map { it.id })
        assertEquals(sourceDao.getAllCurrencies().map { it.key }, dao.getAllCurrencies().map { it.key })
        assertEquals(sourceDao.getAllCategories().map { it.id }, dao.getAllCategories().map { it.id })
        assertEquals(sourceDao.getAllTransactions().map { it.id }, dao.getAllTransactions().map { it.id })
        assertEquals(100, dao.getAllTransactions().single().amountMinor)
        assertEquals(1, dao.getSettings()?.currentLedgerId ?: 0)
        sourceDb.close()
    }

    @Test
    fun restoreReplacesExistingData() = runBlocking {
        val (sourceDb, sourceDao) = sourceDatabase()
        val prepared = BackupManager(context, sourceDao).parse(
            BackupManager(context, sourceDao).export("pw"),
            "pw",
        )

        dao.seedDefaults()
        dao.insertAccount(
            AccountEntity(
                name = "额外账户",
                type = AccountType.CASH,
                typeKey = "cash",
                currencyKey = "cny",
                openingBalanceMinor = 0,
                sortOrder = 9,
                iconKey = "cash",
                isDefault = false,
                isArchived = false,
            ),
        )
        assertEquals(2, dao.getAllAccounts().size)

        BackupManager(context, dao).apply(prepared)
        assertEquals(1, dao.getAllAccounts().size)
        assertEquals("现金", dao.getAllAccounts().single().name)
        sourceDb.close()
    }

    @Test
    fun ledgerCoverMediaRestoredToAppPrivateFile() = runBlocking {
        val (sourceDb, sourceDao) = sourceDatabase()
        val coverFile = File(context.cacheDir, "cover_test.jpg")
        coverFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val ledger = sourceDao.getAllLedgers().single()
        sourceDao.updateLedgerRow(ledger.copy(coverKey = "cover_file:${coverFile.absolutePath}"))

        val prepared = BackupManager(context, sourceDao).parse(
            BackupManager(context, sourceDao).export("pw"),
            "pw",
        )
        assertEquals(1, prepared.summary.mediaCount)

        BackupManager(context, dao).apply(prepared)
        val restored = dao.getAllLedgers().single().coverKey
        assertTrue(restored.startsWith("cover_file:"))
        val restoredFile = File(restored.removePrefix("cover_file:"))
        assertTrue(restoredFile.exists())
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), restoredFile.readBytes())
        sourceDb.close()
        coverFile.delete()
        Unit
    }
}
