package com.vos.accounting.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.vos.accounting.model.AccountType
import com.vos.accounting.model.CategoryTotal
import com.vos.accounting.model.OverviewTotals
import com.vos.accounting.model.TransactionSource
import com.vos.accounting.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * 表示数据库中的资金账户。
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    @ColumnInfo(name = "type_key")
    val typeKey: String,
    @ColumnInfo(name = "currency_key")
    val currencyKey: String,
    @ColumnInfo(name = "opening_balance_minor")
    val openingBalanceMinor: Long,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "icon_key")
    val iconKey: String = "cash",
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
)

/**
 * 表示账户可选的预置或自定义币种及其兑人民币汇率。
 */
@Entity(
    tableName = "currencies",
    indices = [Index(value = ["name"], unique = true)],
)
data class CurrencyEntity(
    @PrimaryKey
    val key: String,
    val code: String,
    val name: String,
    val symbol: String,
    @ColumnInfo(name = "rate_to_cny_scaled")
    val rateToCnyScaled: Long,
    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0,
    @ColumnInfo(name = "auto_rate_enabled")
    val autoRateEnabled: Boolean = true,
)

/**
 * 返回内置币种及用于离线首启的兑人民币汇率。
 */
fun defaultCurrencies(): List<CurrencyEntity> = listOf(
    CurrencyEntity("cny", "CNY", "人民币", "¥", 100_000_000L, true),
    CurrencyEntity("usd", "USD", "美元", "$", 675_977_000L, true),
    CurrencyEntity("eur", "EUR", "欧元", "€", 779_545_000L, true),
    CurrencyEntity("gbp", "GBP", "英镑", "£", 911_070_000L, true),
    CurrencyEntity("jpy", "JPY", "日元", "¥", 4_274_000L, true),
    CurrencyEntity("hkd", "HKD", "港币", "HK$", 86_177_000L, true),
    CurrencyEntity("mop", "MOP", "澳门元", "MOP$", 83_668_000L, true),
    CurrencyEntity("twd", "TWD", "新台币", "NT$", 20_830_000L, true),
    CurrencyEntity("krw", "KRW", "韩元", "₩", 473_500L, true),
    CurrencyEntity("sgd", "SGD", "新加坡元", "S$", 527_343_000L, true),
    CurrencyEntity("aud", "AUD", "澳元", "A$", 475_441_000L, true),
    CurrencyEntity("cad", "CAD", "加拿大元", "C$", 482_179_000L, true),
    CurrencyEntity("chf", "CHF", "瑞士法郎", "CHF", 836_901_000L, true),
    CurrencyEntity("rub", "RUB", "俄罗斯卢布", "₽", 8_460_000L, true),
    CurrencyEntity("thb", "THB", "泰铢", "฿", 20_242_000L, true),
)

/**
 * 表示可由用户扩展的账户类型定义。
 */
@Entity(
    tableName = "account_types",
    indices = [Index(value = ["name"], unique = true)],
)
data class AccountTypeEntity(
    @PrimaryKey
    val key: String,
    val name: String,
    val summary: String,
    @ColumnInfo(name = "icon_key")
    val iconKey: String,
    @ColumnInfo(name = "base_type")
    val baseType: AccountType,
    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean = false,
)

/**
 * 返回首次建库和数据库迁移共用的七种预置账户类型。
 */
fun defaultAccountTypes(): List<AccountTypeEntity> = listOf(
    AccountTypeEntity("cash", "现金", "持有的现金资产", "cash", AccountType.CASH, true),
    AccountTypeEntity("bank_card", "储蓄卡", "银行储蓄卡、借记卡", "debit_card", AccountType.BANK_CARD, true),
    AccountTypeEntity("credit", "信用账户", "银行信用卡、花呗、美团月付等", "credit_card", AccountType.CREDIT, true),
    AccountTypeEntity("online", "网络账户", "支付宝、微信钱包、QQ 钱包等", "online_account", AccountType.ONLINE, true),
    AccountTypeEntity("investment", "投资账户", "理财、基金、股票等", "investment_account", AccountType.INVESTMENT, true),
    AccountTypeEntity("stored_value", "储值卡", "购物卡、饭卡、加油卡等", "stored_value_card", AccountType.STORED_VALUE, true),
    AccountTypeEntity("virtual", "虚拟账户", "积分、游戏币等虚拟资产", "virtual_account", AccountType.VIRTUAL, true),
)

/**
 * 表示数据库中的收支分类。
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "icon_key")
    val iconKey: String = "more",
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
)

/**
 * 表示应用外观设置的单行配置。
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "theme_mode")
    val themeMode: String = "SYSTEM",
    @ColumnInfo(name = "follow_system_color")
    val followSystemColor: Boolean = true,
    @ColumnInfo(name = "predictive_back_animation_enabled", defaultValue = "0")
    val predictiveBackAnimationEnabled: Boolean = false,
)

/**
 * 表示数据库中的单笔账目。
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("account_id"),
        Index("category_id"),
        Index("occurred_at"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val merchant: String,
    val note: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    val source: TransactionSource,
)

/**
 * 表示带账户与分类名称的账目展示记录。
 */
data class TransactionRecord(
    val id: Long,
    val type: TransactionType,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val merchant: String,
    val note: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    val source: TransactionSource,
    @ColumnInfo(name = "account_name")
    val accountName: String,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "category_icon_key")
    val categoryIconKey: String,
    @ColumnInfo(name = "currency_key")
    val currencyKey: String,
    @ColumnInfo(name = "currency_symbol")
    val currencySymbol: String,
    @ColumnInfo(name = "currency_rate_to_cny_scaled")
    val currencyRateToCnyScaled: Long,
)

/**
 * 定义账户、分类和账目的本地数据库操作。
 */
@Dao
interface AccountingDao {
    /**
     * 持续观察全部账户。
     */
    @Query("SELECT * FROM accounts ORDER BY sort_order, id")
    fun observeAccounts(): Flow<List<AccountEntity>>

    /**
     * 持续观察全部账户类型。
     */
    @Query("SELECT * FROM account_types ORDER BY is_builtin DESC, rowid")
    fun observeAccountTypes(): Flow<List<AccountTypeEntity>>

    /**
     * 持续观察全部币种。
     */
    @Query("SELECT * FROM currencies ORDER BY is_builtin DESC, rowid")
    fun observeCurrencies(): Flow<List<CurrencyEntity>>

    /**
     * 持续观察全部分类。
     */
    @Query("SELECT * FROM categories ORDER BY type, sort_order, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    /**
     * 持续观察按时间倒序排列的账目。
     */
    @Query(
        """
        SELECT transactions.*, accounts.name AS account_name, categories.name AS category_name,
            categories.icon_key AS category_icon_key, currencies.`key` AS currency_key,
            currencies.symbol AS currency_symbol,
            currencies.rate_to_cny_scaled AS currency_rate_to_cny_scaled
        FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.account_id
        INNER JOIN categories ON categories.id = transactions.category_id
        INNER JOIN currencies ON currencies.`key` = accounts.currency_key
        ORDER BY occurred_at DESC, transactions.id DESC
        """,
    )
    fun observeTransactions(): Flow<List<TransactionRecord>>

    /**
     * 持续观察全部账目的收支汇总。
     */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN transactions.type = 'INCOME' THEN
                (transactions.amount_minor * currencies.rate_to_cny_scaled + 50000000) / 100000000
                ELSE 0 END), 0) AS income_minor,
            COALESCE(SUM(CASE WHEN transactions.type = 'EXPENSE' THEN
                (transactions.amount_minor * currencies.rate_to_cny_scaled + 50000000) / 100000000
                ELSE 0 END), 0) AS expense_minor
        FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.account_id
        INNER JOIN currencies ON currencies.`key` = accounts.currency_key
        """,
    )
    fun observeOverviewTotals(): Flow<OverviewTotals>

    /**
     * 持续观察支出分类汇总。
     */
    @Query(
        """
        SELECT categories.name AS category_name,
            SUM((transactions.amount_minor * currencies.rate_to_cny_scaled + 50000000) / 100000000) AS amount_minor
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.category_id
        INNER JOIN accounts ON accounts.id = transactions.account_id
        INNER JOIN currencies ON currencies.`key` = accounts.currency_key
        WHERE transactions.type = 'EXPENSE'
        GROUP BY categories.id
        ORDER BY amount_minor DESC
        """,
    )
    fun observeExpenseCategoryTotals(): Flow<List<CategoryTotal>>

    /**
     * 持续观察应用外观设置。
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    /**
     * 写入应用外观设置。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    /**
     * 只更新应用外观的明暗模式。
     */
    @Query("UPDATE app_settings SET theme_mode = :themeMode WHERE id = 1")
    suspend fun updateThemeMode(themeMode: String)

    /**
     * 只更新应用是否跟随系统配色。
     */
    @Query("UPDATE app_settings SET follow_system_color = :followSystemColor WHERE id = 1")
    suspend fun updateFollowSystemColor(followSystemColor: Boolean)

    /**
     * 只更新应用是否启用预测性返回动画。
     */
    @Query("UPDATE app_settings SET predictive_back_animation_enabled = :enabled WHERE id = 1")
    suspend fun updatePredictiveBackAnimationEnabled(enabled: Boolean)

    /**
     * 返回设置行数量。
     */
    @Query("SELECT COUNT(*) FROM app_settings")
    suspend fun countSettings(): Int

    /**
     * 返回指定账户，找不到时返回空。
     */
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun findAccount(accountId: Long): AccountEntity?

    /**
     * 返回指定账户类型，找不到时返回空。
     */
    @Query("SELECT * FROM account_types WHERE `key` = :typeKey")
    suspend fun findAccountType(typeKey: String): AccountTypeEntity?

    /**
     * 返回指定币种，找不到时返回空。
     */
    @Query("SELECT * FROM currencies WHERE `key` = :currencyKey")
    suspend fun findCurrency(currencyKey: String): CurrencyEntity?

    /**
     * 返回指定分类，找不到时返回空。
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun findCategory(categoryId: Long): CategoryEntity?

    /**
     * 返回指定账目，找不到时返回空。
     */
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun findTransaction(transactionId: Long): TransactionEntity?

    /**
     * 插入一个账户。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: AccountEntity): Long

    /**
     * 插入一个自定义账户类型。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountType(accountType: AccountTypeEntity): Long

    /**
     * 插入一组预置账户类型。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountTypes(accountTypes: List<AccountTypeEntity>)

    /**
     * 插入一组预置币种。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    /**
     * 插入一个自定义币种。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCurrency(currency: CurrencyEntity): Long

    /**
     * 更新币种的自动汇率与更新时间。
     */
    @Query("UPDATE currencies SET rate_to_cny_scaled = :rate, updated_at = :updatedAt WHERE code = :code AND is_builtin = 1")
    suspend fun updateBuiltinCurrencyRate(code: String, rate: Long, updatedAt: Long)

    /** 更新币种的名称、符号和手动汇率。 */
    @Query("UPDATE currencies SET name = :name, symbol = :symbol, rate_to_cny_scaled = :rate, updated_at = :updatedAt WHERE `key` = :currencyKey")
    suspend fun updateCurrency(currencyKey: String, name: String, symbol: String, rate: Long, updatedAt: Long): Int

    /** 设置预置币种是否自动刷新汇率。 */
    @Query("UPDATE currencies SET auto_rate_enabled = :enabled WHERE `key` = :currencyKey AND is_builtin = 1")
    suspend fun updateCurrencyAutoRate(currencyKey: String, enabled: Boolean): Int

    /** 返回使用指定币种的账户数量。 */
    @Query("SELECT COUNT(*) FROM accounts WHERE currency_key = :currencyKey")
    suspend fun countAccountsByCurrencyKey(currencyKey: String): Int

    /** 删除未被账户引用的自定义币种。 */
    @Query("DELETE FROM currencies WHERE `key` = :currencyKey AND is_builtin = 0")
    suspend fun deleteCustomCurrency(currencyKey: String): Int

    /** 返回除指定币种外使用相同名称的币种数量。 */
    @Query("SELECT COUNT(*) FROM currencies WHERE `key` != :currencyKey AND name = :name")
    suspend fun countOtherCurrenciesByName(currencyKey: String, name: String): Int

    /**
     * 返回引用指定账户类型的账户数量。
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE type_key = :typeKey")
    suspend fun countAccountsByTypeKey(typeKey: String): Int

    /**
     * 删除一个自定义账户类型。
     */
    @Query("DELETE FROM account_types WHERE `key` = :typeKey AND is_builtin = 0")
    suspend fun deleteCustomAccountType(typeKey: String): Int

    /**
     * 返回除指定标识外使用相同名称的账户类型数量。
     */
    @Query("SELECT COUNT(*) FROM account_types WHERE name = :name AND `key` != :typeKey")
    suspend fun countOtherAccountTypesByName(typeKey: String, name: String): Int

    /**
     * 更新一个自定义账户类型的名称与说明。
     */
    @Query(
        "UPDATE account_types SET name = :name, summary = :summary " +
            "WHERE `key` = :typeKey AND is_builtin = 0",
    )
    suspend fun updateCustomAccountType(typeKey: String, name: String, summary: String): Int

    /**
     * 更新一个已有账户。
     */
    @Update
    suspend fun updateAccount(account: AccountEntity)

    /**
     * 清除已有默认账户标记。
     */
    @Query("UPDATE accounts SET is_default = 0")
    suspend fun clearDefaultAccounts()

    /**
     * 返回未停用的默认账户数量。
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE is_default = 1 AND is_archived = 0")
    suspend fun countActiveDefaultAccounts(): Int

    /**
     * 返回排序最靠前的未停用账户标识。
     */
    @Query("SELECT id FROM accounts WHERE is_archived = 0 ORDER BY sort_order, id LIMIT 1")
    suspend fun firstActiveAccountId(): Long?

    /**
     * 将指定账户设为默认账户。
     */
    @Query("UPDATE accounts SET is_default = 1 WHERE id = :accountId")
    suspend fun markAccountDefault(accountId: Long)

    /**
     * 返回账户当前最大的排序值。
     */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM accounts")
    suspend fun maxAccountSortOrder(): Int

    /**
     * 新增或更新账户，并保持有效账户拥有唯一默认项。
     */
    @Transaction
    suspend fun saveAccount(account: AccountEntity): Long {
        if (account.isDefault && !account.isArchived) clearDefaultAccounts()
        val accountId = if (account.id == 0L) {
            insertAccount(account)
        } else {
            updateAccount(account)
            account.id
        }
        ensureDefaultAccount()
        return accountId
    }

    /**
     * 标记指定账户为停用并清除其默认状态。
     */
    @Query("UPDATE accounts SET is_archived = 1, is_default = 0 WHERE id = :accountId")
    suspend fun markAccountArchived(accountId: Long)

    /**
     * 停用指定账户并为剩余有效账户补齐默认项。
     */
    @Transaction
    suspend fun archiveAccount(accountId: Long) {
        markAccountArchived(accountId)
        ensureDefaultAccount()
    }

    /**
     * 在存在有效账户但没有默认项时选择排序最靠前的账户。
     */
    @Transaction
    suspend fun ensureDefaultAccount() {
        if (countActiveDefaultAccounts() == 0) {
            firstActiveAccountId()?.let { markAccountDefault(it) }
        }
    }

    /**
     * 插入一组分类。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    /**
     * 插入一个分类并返回其标识。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    /**
     * 更新一个已有分类。
     */
    @Update
    suspend fun updateCategory(category: CategoryEntity)

    /**
     * 返回指定方向分类当前最大的排序值。
     */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM categories WHERE type = :type")
    suspend fun maxCategorySortOrder(type: TransactionType): Int

    /**
     * 停用指定分类。
     */
    @Query("UPDATE categories SET is_archived = 1 WHERE id = :categoryId")
    suspend fun archiveCategory(categoryId: Long)

    /**
     * 插入一笔正式账目。
     */
    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    /**
     * 更新一笔已有账目。
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    /**
     * 删除指定账目。
     */
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long): Int

    /**
     * 返回账户数量。
     */
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int

    /**
     * 返回账户类型数量。
     */
    @Query("SELECT COUNT(*) FROM account_types")
    suspend fun countAccountTypes(): Int

    /**
     * 返回币种数量。
     */
    @Query("SELECT COUNT(*) FROM currencies")
    suspend fun countCurrencies(): Int

    /**
     * 返回分类数量。
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    /**
     * 在首次启动时建立默认账户与分类。
     */
    @Transaction
    suspend fun seedDefaults() {
        if (countAccountTypes() == 0) {
            insertAccountTypes(defaultAccountTypes())
        }
        if (countCurrencies() == 0) {
            insertCurrencies(defaultCurrencies())
        }
        if (countAccounts() == 0) {
            insertAccount(
                AccountEntity(
                    name = "现金",
                    type = AccountType.CASH,
                    typeKey = "cash",
                    currencyKey = "cny",
                    openingBalanceMinor = 0,
                    sortOrder = 0,
                    isDefault = true,
                ),
            )
        }
        ensureDefaultAccount()
        if (countSettings() == 0) {
            upsertSettings(AppSettingsEntity())
        }
        if (countCategories() == 0) {
            insertCategories(
                listOf(
                    CategoryEntity(name = "餐饮", type = TransactionType.EXPENSE, sortOrder = 0, iconKey = "store"),
                    CategoryEntity(name = "交通", type = TransactionType.EXPENSE, sortOrder = 1, iconKey = "carrier"),
                    CategoryEntity(name = "购物", type = TransactionType.EXPENSE, sortOrder = 2, iconKey = "bank_cards"),
                    CategoryEntity(name = "居住", type = TransactionType.EXPENSE, sortOrder = 3, iconKey = "home"),
                    CategoryEntity(name = "娱乐", type = TransactionType.EXPENSE, sortOrder = 4, iconKey = "music"),
                    CategoryEntity(name = "工资", type = TransactionType.INCOME, sortOrder = 0, iconKey = "bank_cards"),
                    CategoryEntity(name = "奖金", type = TransactionType.INCOME, sortOrder = 1, iconKey = "promotions"),
                    CategoryEntity(name = "其他收入", type = TransactionType.INCOME, sortOrder = 2, iconKey = "more"),
                ),
            )
        }
    }
}

/**
 * 提供应用的 Room 数据库。
 */
@Database(
    entities = [
        AccountEntity::class,
        AccountTypeEntity::class,
        CurrencyEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        AppSettingsEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AccountingDatabase : RoomDatabase() {
    /**
     * 提供记账数据访问对象。
     */
    abstract fun accountingDao(): AccountingDao

    companion object {
        /**
         * 创建应用进程内唯一的数据库实例。
         */
        fun create(context: Context): AccountingDatabase = Room.databaseBuilder(
            context,
            AccountingDatabase::class.java,
            "accounting.db",
        ).addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
        ).build()

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            /**
             * 为账户和分类补充管理状态及分类图标字段。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "ALTER TABLE accounts ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0",
                )
                connection.executeMigrationSql(
                    "ALTER TABLE accounts ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0",
                )
                connection.executeMigrationSql(
                    "UPDATE accounts SET is_default = 1 WHERE id = (SELECT MIN(id) FROM accounts)",
                )
                connection.executeMigrationSql(
                    "ALTER TABLE categories ADD COLUMN icon_key TEXT NOT NULL DEFAULT 'more'",
                )
                connection.executeMigrationSql(
                    "ALTER TABLE categories ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0",
                )
                connection.executeMigrationSql(
                    """
                    UPDATE categories SET icon_key = CASE name
                        WHEN '餐饮' THEN 'store'
                        WHEN '交通' THEN 'carrier'
                        WHEN '购物' THEN 'bank_cards'
                        WHEN '居住' THEN 'home'
                        WHEN '娱乐' THEN 'music'
                        WHEN '工资' THEN 'bank_cards'
                        WHEN '奖金' THEN 'promotions'
                        ELSE 'more'
                    END
                    """.trimIndent(),
                )
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            /**
             * 建立应用外观设置表并写入默认值。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `theme_mode` TEXT NOT NULL, `follow_system_color` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                connection.executeMigrationSql(
                    "INSERT OR REPLACE INTO `app_settings` (`id`, `theme_mode`, `follow_system_color`) VALUES (1, 'SYSTEM', 1)",
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            /**
             * 为账户补充可独立选择并持久化的图标标识。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "ALTER TABLE accounts ADD COLUMN icon_key TEXT NOT NULL DEFAULT 'cash'",
                )
                connection.executeMigrationSql(
                    """
                    UPDATE accounts SET icon_key = CASE type
                        WHEN 'CASH' THEN 'cash'
                        WHEN 'BANK_CARD' THEN 'debit_card'
                        WHEN 'CREDIT' THEN 'credit_card'
                        WHEN 'ONLINE' THEN 'online_account'
                        WHEN 'INVESTMENT' THEN 'investment_account'
                        WHEN 'STORED_VALUE' THEN 'stored_value_card'
                        WHEN 'VIRTUAL' THEN 'virtual_account'
                        ELSE 'cash'
                    END
                    """.trimIndent(),
                )
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            /**
             * 建立可扩展账户类型表并为已有账户补充类型标识。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `account_types` (
                        `key` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `icon_key` TEXT NOT NULL,
                        `base_type` TEXT NOT NULL,
                        `is_builtin` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_account_types_name` ON `account_types` (`name`)",
                )
                defaultAccountTypes().forEach { type ->
                    connection.executeMigrationSql(
                        "INSERT OR IGNORE INTO account_types (`key`, name, summary, icon_key, base_type, is_builtin) " +
                            "VALUES ('${type.key}', '${type.name}', '${type.summary}', '${type.iconKey}', '${type.baseType.name}', 1)",
                    )
                }
                connection.executeMigrationSql(
                    "ALTER TABLE accounts ADD COLUMN type_key TEXT NOT NULL DEFAULT 'cash'",
                )
                connection.executeMigrationSql(
                    """
                    UPDATE accounts SET type_key = CASE type
                        WHEN 'CASH' THEN 'cash'
                        WHEN 'BANK_CARD' THEN 'bank_card'
                        WHEN 'CREDIT' THEN 'credit'
                        WHEN 'ONLINE' THEN 'online'
                        WHEN 'INVESTMENT' THEN 'investment'
                        WHEN 'STORED_VALUE' THEN 'stored_value'
                        WHEN 'VIRTUAL' THEN 'virtual'
                        ELSE 'cash'
                    END
                    """.trimIndent(),
                )
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            /**
             * 建立币种与定点汇率表，并将已有账户迁移为人民币账户。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `currencies` (
                        `key` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `rate_to_cny_scaled` INTEGER NOT NULL,
                        `is_builtin` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_currencies_name` ON `currencies` (`name`)",
                )
                defaultCurrencies().forEach { currency ->
                    connection.executeMigrationSql(
                        "INSERT OR IGNORE INTO currencies (`key`, code, name, symbol, rate_to_cny_scaled, is_builtin, updated_at) " +
                            "VALUES ('${currency.key}', '${currency.code}', '${currency.name}', '${currency.symbol}', " +
                            "${currency.rateToCnyScaled}, 1, 0)",
                    )
                }
                connection.executeMigrationSql(
                    "ALTER TABLE accounts ADD COLUMN currency_key TEXT NOT NULL DEFAULT 'cny'",
                )
            }
        }

        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            /** 为币种补充自动汇率开关，并保持已有自定义币种使用手动汇率。 */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "ALTER TABLE currencies ADD COLUMN auto_rate_enabled INTEGER NOT NULL DEFAULT 1",
                )
                connection.executeMigrationSql(
                    "UPDATE currencies SET auto_rate_enabled = 0 WHERE is_builtin = 0",
                )
            }
        }

        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            /**
             * 为应用设置补充默认关闭的预测性返回动画开关。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "ALTER TABLE app_settings ADD COLUMN predictive_back_animation_enabled INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}

/**
 * 执行一条不返回结果的数据库迁移语句。
 */
private fun SQLiteConnection.executeMigrationSql(sql: String) {
    val statement = prepare(sql)
    try {
        statement.step()
    } finally {
        statement.close()
    }
}
