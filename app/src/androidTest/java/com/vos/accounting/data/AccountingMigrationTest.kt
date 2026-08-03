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

    /**
     * 验证 v3 账户升级后按原账户类型获得独立图标标识。
     */
    @Test
    fun migrateVersionThreeToVersionFour() {
        helper.createDatabase(DATABASE_NAME, 3).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, opening_balance_minor, sort_order, is_default, is_archived) " +
                    "VALUES (1, '银行卡', 'BANK_CARD', 5000, 0, 1, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            AccountingDatabase.MIGRATION_3_4,
        )
        migrated.query("SELECT icon_key FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("debit_card", it.getString(0))
        }
        migrated.close()
    }

    /**
     * 验证 v4 账户升级后建立可扩展类型表并保留原类型映射。
     */
    @Test
    fun migrateVersionFourToVersionFive() {
        helper.createDatabase(DATABASE_NAME, 4).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, opening_balance_minor, sort_order, icon_key, is_default, is_archived) " +
                    "VALUES (1, '网络钱包', 'ONLINE', 5000, 0, 'wechat_wallet', 1, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            5,
            true,
            AccountingDatabase.MIGRATION_4_5,
        )
        migrated.query("SELECT type_key FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("online", it.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM account_types").use {
            it.moveToFirst()
            assertEquals(7, it.getInt(0))
        }
        migrated.close()
    }

    /** 验证 v5 账户升级后默认使用人民币并写入全部预置币种。 */
    @Test
    fun migrateVersionFiveToVersionSix() {
        helper.createDatabase(DATABASE_NAME, 5).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, opening_balance_minor, sort_order, icon_key, is_default, is_archived, type_key) " +
                    "VALUES (1, '现金', 'CASH', 5000, 0, 'cash', 1, 0, 'cash')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            6,
            true,
            AccountingDatabase.MIGRATION_5_6,
        )
        migrated.query("SELECT currency_key FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("cny", it.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM currencies").use {
            it.moveToFirst()
            assertEquals(15, it.getInt(0))
        }
        migrated.close()
    }

    /** 验证 v6 币种升级后预置币种自动更新、自定义币种保持手动汇率。 */
    @Test
    fun migrateVersionSixToVersionSeven() {
        helper.createDatabase(DATABASE_NAME, 6).apply {
            execSQL(
                "INSERT INTO currencies (`key`, code, name, symbol, rate_to_cny_scaled, is_builtin, updated_at) " +
                    "VALUES ('cny', 'CNY', '人民币', '¥', 100000000, 1, 0)",
            )
            execSQL(
                "INSERT INTO currencies (`key`, code, name, symbol, rate_to_cny_scaled, is_builtin, updated_at) " +
                    "VALUES ('custom_test', '', '测试币', 'T', 200000000, 0, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            7,
            true,
            AccountingDatabase.MIGRATION_6_7,
        )
        migrated.query("SELECT auto_rate_enabled FROM currencies WHERE `key` = 'cny'").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.query("SELECT auto_rate_enabled FROM currencies WHERE `key` = 'custom_test'").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.close()
    }

    /**
     * 验证 v1 数据库经过连续迁移后完整升级到当前版本。
     */
    @Test
    fun migrateVersionOneToVersionSeven() {
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
            7,
            true,
            AccountingDatabase.MIGRATION_1_2,
            AccountingDatabase.MIGRATION_2_3,
            AccountingDatabase.MIGRATION_3_4,
            AccountingDatabase.MIGRATION_4_5,
            AccountingDatabase.MIGRATION_5_6,
            AccountingDatabase.MIGRATION_6_7,
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
        migrated.query("SELECT theme_mode, follow_system_color FROM app_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("SYSTEM", it.getString(0))
            assertEquals(1, it.getInt(1))
        }
        migrated.query("SELECT icon_key FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("cash", it.getString(0))
        }
        migrated.query("SELECT type_key FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("cash", it.getString(0))
        }
        migrated.query("SELECT currency_key FROM accounts WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("cny", it.getString(0))
        }
        migrated.query("SELECT auto_rate_enabled FROM currencies WHERE `key` = 'cny'").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "accounting_migration_test"
    }
}
