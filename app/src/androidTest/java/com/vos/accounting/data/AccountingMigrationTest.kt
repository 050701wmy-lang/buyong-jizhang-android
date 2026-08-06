package com.vos.accounting.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
     * 验证 v7 设置升级后新增的预测性返回动画默认关闭。
     */
    @Test
    fun migrateVersionSevenToVersionEight() {
        helper.createDatabase(DATABASE_NAME, 7).apply {
            execSQL(
                "INSERT INTO app_settings (id, theme_mode, follow_system_color) " +
                    "VALUES (1, 'DARK', 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            8,
            true,
            AccountingDatabase.MIGRATION_7_8,
        )
        migrated.query(
            "SELECT theme_mode, follow_system_color, predictive_back_animation_enabled " +
                "FROM app_settings WHERE id = 1",
        ).use {
            it.moveToFirst()
            assertEquals("DARK", it.getString(0))
            assertEquals(0, it.getInt(1))
            assertEquals(0, it.getInt(2))
        }
        migrated.close()
    }

    /** 验证 v8 数据升级后自动建立默认账本并关联已有账户。 */
    @Test
    fun migrateVersionEightToVersionNine() {
        helper.createDatabase(DATABASE_NAME, 8).apply {
            execSQL(
                "INSERT INTO accounts (id, name, type, opening_balance_minor, sort_order, icon_key, is_default, is_archived, type_key, currency_key) " +
                    "VALUES (1, '现金', 'CASH', 5000, 0, 'cash', 1, 0, 'cash', 'cny')",
            )
            execSQL(
                "INSERT INTO app_settings (id, theme_mode, follow_system_color, predictive_back_animation_enabled) VALUES (1, 'SYSTEM', 1, 0)",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            9,
            true,
            AccountingDatabase.MIGRATION_8_9,
        )
        migrated.query("SELECT name, base_currency_key FROM ledgers WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("日常账本", it.getString(0))
            assertEquals("cny", it.getString(1))
        }
        migrated.query("SELECT ledger_id FROM account_ledger_cross_ref WHERE account_id = 1").use {
            it.moveToFirst()
            assertEquals(1, it.getLong(0))
        }
        migrated.close()
    }

    /** 验证 v9 数据升级后保留账户-账本关联并修复失效的本位币引用。 */
    @Test
    fun migrateVersionNineToVersionTen() {
        helper.createDatabase(DATABASE_NAME, 9).apply {
            execSQL(
                "INSERT INTO currencies (`key`, code, name, symbol, rate_to_cny_scaled, is_builtin, updated_at, auto_rate_enabled) " +
                    "VALUES ('cny','CNY','人民币','¥',100000,1,0,1),('usd','USD','美元','$',720000,1,0,1)",
            )
            execSQL(
                "INSERT INTO ledgers (id, name, cover_key, use_light_text, base_currency_key, is_hidden, sort_order) " +
                    "VALUES (1,'日常账本','cover_ocean',1,'cny',0,0),(2,'旅行账本','cover_sunset',0,'zzz',0,1),(3,'隐藏账本','cover_forest',1,'cny',1,2)",
            )
            execSQL(
                "INSERT INTO accounts (id, name, type, type_key, currency_key, opening_balance_minor, sort_order, icon_key, is_default, is_archived) " +
                    "VALUES (1,'现金','CASH','cash','cny',5000,0,'cash',1,0),(2,'美元卡','BANK_CARD','bank_card','usd',0,1,'debit_card',0,0)",
            )
            execSQL(
                "INSERT INTO account_ledger_cross_ref (account_id, ledger_id) VALUES (1,1),(1,2),(2,2),(2,3)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            10,
            true,
            AccountingDatabase.MIGRATION_9_10,
        )
        migrated.query("SELECT COUNT(*) FROM account_ledger_cross_ref").use {
            it.moveToFirst()
            assertEquals(4, it.getInt(0))
        }
        migrated.query("SELECT base_currency_key FROM ledgers WHERE id = 2").use {
            it.moveToFirst()
            assertEquals("cny", it.getString(0))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name IN " +
                "('index_ledgers_name', 'index_ledgers_base_currency_key', 'index_account_ledger_cross_ref_ledger_id')",
        ).use {
            it.moveToFirst()
            assertEquals(3, it.getInt(0))
        }
        migrated.close()
    }

    /** 验证 v10 数据升级后为当前账本与账目归属补充外键并修复悬空引用。 */
    @Test
    fun migrateVersionTenToVersionEleven() {
        helper.createDatabase(DATABASE_NAME, 10).apply {
            execSQL(
                "INSERT INTO currencies (`key`, code, name, symbol, rate_to_cny_scaled, is_builtin, updated_at, auto_rate_enabled) " +
                    "VALUES ('cny','CNY','人民币','¥',100000,1,0,1)",
            )
            execSQL(
                "INSERT INTO ledgers (id, name, cover_key, use_light_text, base_currency_key, is_hidden, sort_order) " +
                    "VALUES (1,'日常账本','cover_ocean',1,'cny',0,0),(2,'旅行账本','cover_sunset',0,'cny',0,1)",
            )
            execSQL(
                "INSERT INTO accounts (id, name, type, type_key, currency_key, opening_balance_minor, sort_order, icon_key, is_default, is_archived) " +
                    "VALUES (1,'现金','CASH','cash','cny',5000,0,'cash',1,0)",
            )
            execSQL(
                "INSERT INTO categories (id, name, type, sort_order, icon_key, is_archived) " +
                    "VALUES (1,'餐饮','EXPENSE',0,'store',0)",
            )
            execSQL(
                "INSERT INTO app_settings (id, theme_mode, follow_system_color, predictive_back_animation_enabled, current_ledger_id) " +
                    "VALUES (1,'SYSTEM',1,0,99)",
            )
            execSQL(
                "INSERT INTO transactions (id, type, amount_minor, account_id, category_id, merchant, note, occurred_at, source, ledger_id) " +
                    "VALUES (1,'EXPENSE',100,1,1,'','',1,'MANUAL',99),(2,'EXPENSE',200,1,1,'','',2,'MANUAL',2)",
            )
            execSQL(
                "INSERT INTO account_ledger_cross_ref (account_id, ledger_id) VALUES (1,1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            11,
            true,
            AccountingDatabase.MIGRATION_10_11,
        )
        migrated.query("SELECT current_ledger_id FROM app_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1, it.getLong(0))
        }
        migrated.query("SELECT ledger_id FROM transactions WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1, it.getLong(0))
        }
        migrated.query("SELECT ledger_id FROM transactions WHERE id = 2").use {
            it.moveToFirst()
            assertEquals(2, it.getLong(0))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_ledger_id_occurred_at_id'",
        ).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            migrated.execSQL(
                "INSERT INTO transactions (type, amount_minor, account_id, category_id, merchant, note, occurred_at, source, ledger_id) " +
                    "VALUES ('EXPENSE', 1, 1, 1, '', '', 3, 'MANUAL', 99)",
            )
        }
        migrated.close()
    }

    /** 验证 v11 数据升级后固化币种与本位币金额快照。 */
    @Test
    fun migrateVersionElevenToVersionTwelve() {
        helper.createDatabase(DATABASE_NAME, 11).apply {
            execSQL(
                "INSERT INTO currencies (`key`, code, name, symbol, rate_to_cny_scaled, is_builtin, updated_at, auto_rate_enabled) " +
                    "VALUES ('cny','CNY','人民币','¥',100000000,1,0,1),('usd','USD','美元','$',675977000,1,0,1)",
            )
            execSQL(
                "INSERT INTO ledgers (id, name, cover_key, use_light_text, base_currency_key, is_hidden, sort_order) " +
                    "VALUES (1,'日常账本','cover_ocean',1,'cny',0,0)",
            )
            execSQL(
                "INSERT INTO accounts (id, name, type, type_key, currency_key, opening_balance_minor, sort_order, icon_key, is_default, is_archived) " +
                    "VALUES (1,'现金','CASH','cash','cny',5000,0,'cash',1,0),(2,'美元卡','BANK_CARD','bank_card','usd',0,1,'debit_card',0,0)",
            )
            execSQL(
                "INSERT INTO categories (id, name, type, sort_order, icon_key, is_archived) " +
                    "VALUES (1,'餐饮','EXPENSE',0,'store',0)",
            )
            execSQL(
                "INSERT INTO transactions (id, type, amount_minor, account_id, category_id, merchant, note, occurred_at, source, ledger_id) " +
                    "VALUES (1,'EXPENSE',10000,2,1,'','',1,'MANUAL',1),(2,'EXPENSE',5000,1,1,'','',2,'MANUAL',1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            12,
            true,
            AccountingDatabase.MIGRATION_11_12,
        )
        migrated.query("SELECT currency_key, base_amount_minor FROM transactions WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("usd", it.getString(0))
            assertEquals(67598, it.getLong(1))
        }
        migrated.query("SELECT currency_key, base_amount_minor FROM transactions WHERE id = 2").use {
            it.moveToFirst()
            assertEquals("cny", it.getString(0))
            assertEquals(5000, it.getLong(1))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_currency_key'",
        ).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            migrated.execSQL("DELETE FROM currencies WHERE `key` = 'usd'")
        }
        migrated.close()
    }

    /**
     * 验证 v1 数据库经过连续迁移后完整升级到当前版本。
     */
    @Test
    fun migrateVersionOneToVersionTwelve() {
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
            12,
            true,
            AccountingDatabase.MIGRATION_1_2,
            AccountingDatabase.MIGRATION_2_3,
            AccountingDatabase.MIGRATION_3_4,
            AccountingDatabase.MIGRATION_4_5,
            AccountingDatabase.MIGRATION_5_6,
            AccountingDatabase.MIGRATION_6_7,
            AccountingDatabase.MIGRATION_7_8,
            AccountingDatabase.MIGRATION_8_9,
            AccountingDatabase.MIGRATION_9_10,
            AccountingDatabase.MIGRATION_10_11,
            AccountingDatabase.MIGRATION_11_12,
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
        migrated.query("SELECT predictive_back_animation_enabled FROM app_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.query("SELECT current_ledger_id FROM app_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1, it.getLong(0))
        }
        migrated.query("SELECT base_currency_key FROM ledgers WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("cny", it.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM account_ledger_cross_ref").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_ledger_id_occurred_at_id'",
        ).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_currency_key'",
        ).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "accounting_migration_test"
    }
}
