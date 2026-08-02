package com.vos.accounting.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证旧版数据库升级到当前 Room schema 后的数据完整性。
 */
@RunWith(AndroidJUnit4::class)
class AccountingMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AccountingDatabase::class.java,
    )

    /**
     * 验证 v1 账户和分类升级后获得正确的默认值与图标。
     */
    @Test
    fun migrateVersionOneToVersionTwo() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, opening_balance_minor, sort_order) " +
                    "VALUES (1, '现金', 'CASH', 5000, 0)",
            )
            execSQL(
                "INSERT INTO categories (id, name, type, sort_order) " +
                    "VALUES (1, '餐饮', 'EXPENSE', 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            2,
            true,
            AccountingDatabase.MIGRATION_1_2,
        )
        migrated.query("SELECT is_default, is_archived FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
            assertEquals(0, it.getInt(1))
        }
        migrated.query("SELECT icon_key, is_archived FROM categories WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("store", it.getString(0))
            assertEquals(0, it.getInt(1))
        }
        migrated.close()
    }

    /**
     * 验证 v2 升级后建立应用外观设置表并写入默认值。
     */
    @Test
    fun migrateVersionTwoToVersionThree() {
        helper.createDatabase(DATABASE_NAME, 2).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, opening_balance_minor, sort_order, is_default, is_archived) " +
                    "VALUES (1, '现金', 'CASH', 5000, 0, 1, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            3,
            true,
            AccountingDatabase.MIGRATION_2_3,
        )
        migrated.query("SELECT theme_mode, follow_system_color FROM app_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("SYSTEM", it.getString(0))
            assertEquals(1, it.getInt(1))
        }
        migrated.query("SELECT COUNT(*) FROM app_settings").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "accounting_migration_test"
    }
}
