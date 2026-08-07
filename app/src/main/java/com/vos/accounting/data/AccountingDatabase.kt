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
import com.vos.accounting.model.TransferDirection
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow

/**
 * 表示数据库中的资金账户。
 */
@Entity(tableName = "accounts")
@Serializable
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

/** 表示可独立筛选账目并提供主题封面的账本。 */
@Entity(
    tableName = "ledgers",
    foreignKeys = [
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["key"],
            childColumns = ["base_currency_key"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["base_currency_key"]),
    ],
)
@Serializable
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "cover_key")
    val coverKey: String,
    @ColumnInfo(name = "use_light_text")
    val useLightText: Boolean,
    @ColumnInfo(name = "base_currency_key")
    val baseCurrencyKey: String,
    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)

/** 表示一个账户可在哪些账本中使用。 */
@Entity(
    tableName = "account_ledger_cross_ref",
    primaryKeys = ["account_id", "ledger_id"],
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LedgerEntity::class, parentColumns = ["id"], childColumns = ["ledger_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("ledger_id")],
)
@Serializable
data class AccountLedgerCrossRef(
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "ledger_id")
    val ledgerId: Long = 1,
)

/** 表示账本选择页需要的账本信息与账目数量。 */
data class LedgerRecord(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "cover_key")
    val coverKey: String,
    @ColumnInfo(name = "use_light_text")
    val useLightText: Boolean,
    @ColumnInfo(name = "base_currency_key")
    val baseCurrencyKey: String,
    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int,
)

/** 表示账本生命周期操作在同一事务中的结果。 */
enum class LedgerMutationResult {
    SUCCESS,
    NOT_FOUND,
    CURRENT_LEDGER,
    HIDDEN_LEDGER,
    DEFAULT_LEDGER,
    HAS_TRANSACTIONS,
    HAS_ACCOUNT_LINKS,
    SETTINGS_MISSING,
    UPDATE_FAILED,
    DELETE_FAILED,
}

/** 表示币种删除在同一事务中的结果。 */
enum class CurrencyDeleteResult {
    SUCCESS,
    NOT_FOUND,
    BUILTIN_CURRENCY,
    ACCOUNT_LINKS,
    LEDGER_LINKS,
    TRANSACTION_LINKS,
    DELETE_FAILED,
}

/**
 * 表示账户可选的预置或自定义币种及其兑人民币汇率。
 */
@Entity(
    tableName = "currencies",
    indices = [Index(value = ["name"], unique = true)],
)
@Serializable
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
@Serializable
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
@Serializable
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
@Entity(
    tableName = "app_settings",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["current_ledger_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("current_ledger_id")],
)
@Serializable
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "theme_mode")
    val themeMode: String = "SYSTEM",
    @ColumnInfo(name = "follow_system_color")
    val followSystemColor: Boolean = true,
    @ColumnInfo(name = "predictive_back_animation_enabled", defaultValue = "0")
    val predictiveBackAnimationEnabled: Boolean = false,
    @ColumnInfo(name = "current_ledger_id", defaultValue = "1")
    val currentLedgerId: Long = 1,
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
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["key"],
            childColumns = ["currency_key"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("account_id"),
        Index("category_id"),
        Index("occurred_at"),
        Index("currency_key"),
        Index(value = ["ledger_id", "occurred_at", "id"]),
    ],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    val merchant: String,
    val note: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    val source: TransactionSource,
    @ColumnInfo(name = "ledger_id")
    val ledgerId: Long = 1,
    @ColumnInfo(name = "currency_key")
    val currencyKey: String = "cny",
    @ColumnInfo(name = "base_amount_minor")
    val baseAmountMinor: Long = 0,
    @ColumnInfo(name = "exchange_id")
    val exchangeId: Long? = null,
    @ColumnInfo(name = "transfer_direction")
    val transferDirection: TransferDirection? = null,
)

/**
 * 表示带账户与分类名称的账目展示记录。
 */
data class TransactionRecord(
    val id: Long,
    val type: TransactionType,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "base_amount_minor")
    val baseAmountMinor: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
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
    @ColumnInfo(name = "ledger_id")
    val ledgerId: Long,
    @ColumnInfo(name = "exchange_id")
    val exchangeId: Long?,
    @ColumnInfo(name = "transfer_direction")
    val transferDirection: TransferDirection?,
)

/**
 * 定义账户、分类和账目的本地数据库操作。
 */

/** 账目查询公共列与连表，供按账本与全量查询复用。 */
private const val TRANSACTION_SELECT =
    """
        SELECT
            transactions.id AS id,
            transactions.type AS type,
            transactions.amount_minor AS amount_minor,
            transactions.base_amount_minor AS base_amount_minor,
            transactions.account_id AS account_id,
            transactions.category_id AS category_id,
            transactions.merchant AS merchant,
            transactions.note AS note,
            transactions.occurred_at AS occurred_at,
            transactions.source AS source,
            transactions.ledger_id AS ledger_id,
            accounts.name AS account_name,
            COALESCE(categories.name, '币种兑换') AS category_name,
            COALESCE(categories.icon_key, '') AS category_icon_key,
            transactions.currency_key AS currency_key,
            currencies.symbol AS currency_symbol,
            currencies.rate_to_cny_scaled AS currency_rate_to_cny_scaled,
            transactions.exchange_id AS exchange_id,
            transactions.transfer_direction AS transfer_direction
        FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.account_id
        LEFT JOIN categories ON categories.id = transactions.category_id
        INNER JOIN currencies ON currencies.`key` = transactions.currency_key
    """

@Dao
interface AccountingDao {
    /**
     * 持续观察全部账户。
     */
    @Query("SELECT accounts.* FROM accounts INNER JOIN account_ledger_cross_ref ref ON ref.account_id = accounts.id INNER JOIN app_settings ON app_settings.id = 1 WHERE ref.ledger_id = app_settings.current_ledger_id ORDER BY accounts.sort_order, accounts.id")
    fun observeAccounts(): Flow<List<AccountEntity>>

    /** 持续观察全部账本及其账目数量。 */
    @Query("SELECT ledgers.*, COUNT(transactions.id) AS transaction_count FROM ledgers LEFT JOIN transactions ON transactions.ledger_id = ledgers.id GROUP BY ledgers.id ORDER BY ledgers.sort_order, ledgers.id")
    fun observeLedgers(): Flow<List<LedgerRecord>>

    /** 持续观察全部账户与账本关联。 */
    @Query("SELECT * FROM account_ledger_cross_ref")
    fun observeAccountLedgerCrossRefs(): Flow<List<AccountLedgerCrossRef>>

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
    @Query(TRANSACTION_SELECT + " WHERE transactions.ledger_id = :ledgerId\n        ORDER BY occurred_at DESC, transactions.id DESC")
    fun observeTransactions(ledgerId: Long): Flow<List<TransactionRecord>>

    /**
     * 持续观察跨全部账本的账目，用于计算账户全局资产。
     */
    @Query(TRANSACTION_SELECT + "\n        ORDER BY occurred_at DESC, transactions.id DESC")
    fun observeAllTransactions(): Flow<List<TransactionRecord>>

    /**
     * 持续观察全部账目的收支汇总。
     */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN transactions.type = 'INCOME' THEN transactions.base_amount_minor ELSE 0 END), 0) AS income_minor,
            COALESCE(SUM(CASE WHEN transactions.type = 'EXPENSE' THEN transactions.base_amount_minor ELSE 0 END), 0) AS expense_minor
        FROM transactions
        WHERE transactions.ledger_id = :ledgerId
        """,
    )
    fun observeOverviewTotals(ledgerId: Long): Flow<OverviewTotals>

    /**
     * 持续观察支出分类汇总。
     */
    @Query(
        """
        SELECT categories.name AS category_name,
            SUM(transactions.base_amount_minor) AS amount_minor
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.category_id
        WHERE transactions.type = 'EXPENSE' AND transactions.ledger_id = :ledgerId
        GROUP BY categories.id
        ORDER BY amount_minor DESC
        """,
    )
    fun observeExpenseCategoryTotals(ledgerId: Long): Flow<List<CategoryTotal>>

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

    /** 返回应用设置中记录的当前账本标识。 */
    @Query("SELECT current_ledger_id FROM app_settings WHERE id = 1")
    suspend fun findCurrentLedgerId(): Long?

    /** 更新应用设置中的当前账本标识。 */
    @Query("UPDATE app_settings SET current_ledger_id = :ledgerId WHERE id = 1")
    suspend fun updateCurrentLedger(ledgerId: Long): Int

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

    /** 返回引用指定账户的历史账目数量。 */
    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun countTransactionsByAccountId(accountId: Long): Int

    /** 返回指定账户的全部账目流水。 */
    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY occurred_at, id")
    suspend fun transactionsByAccount(accountId: Long): List<TransactionEntity>

    /** 返回指定账户排序最靠前的账本关联。 */
    @Query("SELECT ledger_id FROM account_ledger_cross_ref WHERE account_id = :accountId ORDER BY ledger_id LIMIT 1")
    suspend fun firstLedgerIdByAccount(accountId: Long): Long?

    /** 返回全部账户。 */
    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun getAllAccounts(): List<AccountEntity>

    /** 返回全部账本。 */
    @Query("SELECT * FROM ledgers ORDER BY id")
    suspend fun getAllLedgers(): List<LedgerEntity>

    /** 返回全部账户-账本关联。 */
    @Query("SELECT * FROM account_ledger_cross_ref ORDER BY account_id, ledger_id")
    suspend fun getAllAccountLedgerCrossRefs(): List<AccountLedgerCrossRef>

    /** 返回全部账户类型。 */
    @Query("SELECT * FROM account_types ORDER BY `key`")
    suspend fun getAllAccountTypes(): List<AccountTypeEntity>

    /** 返回全部币种。 */
    @Query("SELECT * FROM currencies ORDER BY `key`")
    suspend fun getAllCurrencies(): List<CurrencyEntity>

    /** 返回全部分类。 */
    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAllCategories(): List<CategoryEntity>

    /** 返回全部账目流水。 */
    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getAllTransactions(): List<TransactionEntity>

    /** 返回应用设置单行。 */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    /** 清空全部账目。 */
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    /** 清空全部账户-账本关联。 */
    @Query("DELETE FROM account_ledger_cross_ref")
    suspend fun deleteAllAccountLedgerCrossRefs()

    /** 清空全部分类。 */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    /** 清空全部账户。 */
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    /** 清空全部账户类型。 */
    @Query("DELETE FROM account_types")
    suspend fun deleteAllAccountTypes()

    /** 清空全部账本。 */
    @Query("DELETE FROM ledgers")
    suspend fun deleteAllLedgers()

    /** 清空全部币种。 */
    @Query("DELETE FROM currencies")
    suspend fun deleteAllCurrencies()

    /** 清空应用设置。 */
    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()

    /** 在同一事务中全量替换全部数据，恢复备份时使用。 */
    @Transaction
    suspend fun replaceAll(
        currencies: List<CurrencyEntity>,
        accountTypes: List<AccountTypeEntity>,
        ledgers: List<LedgerEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        crossRefs: List<AccountLedgerCrossRef>,
        transactions: List<TransactionEntity>,
        settings: AppSettingsEntity?,
    ) {
        deleteAllTransactions()
        deleteAllAccountLedgerCrossRefs()
        deleteAllSettings()
        deleteAllCategories()
        deleteAllAccounts()
        deleteAllAccountTypes()
        deleteAllLedgers()
        deleteAllCurrencies()
        if (currencies.isNotEmpty()) insertCurrencies(currencies)
        accountTypes.forEach { insertAccountType(it) }
        ledgers.forEach { insertLedger(it) }
        accounts.forEach { insertAccount(it) }
        if (categories.isNotEmpty()) insertCategories(categories)
        if (crossRefs.isNotEmpty()) insertAccountLedgerCrossRefs(crossRefs)
        transactions.forEach { insertTransaction(it) }
        upsertSettings(settings ?: AppSettingsEntity())
        ensureDefaultAccount()
        ensureCurrentLedger()
    }

    /** 返回引用指定币种的账目数量。 */
    @Query("SELECT COUNT(*) FROM transactions WHERE currency_key = :currencyKey")
    suspend fun countTransactionsByCurrencyKey(currencyKey: String): Int

    /** 返回账户或账本实际使用的币种标识。 */
    @Query(
        "SELECT DISTINCT currency_key FROM accounts UNION SELECT DISTINCT base_currency_key FROM ledgers",
    )
    suspend fun usedCurrencyKeys(): List<String>

    /** 返回指定账本。 */
    @Query("SELECT * FROM ledgers WHERE id = :ledgerId")
    suspend fun findLedger(ledgerId: Long): LedgerEntity?

    /** 返回账户与账本的关联，不存在时返回空。 */
    @Query(
        "SELECT * FROM account_ledger_cross_ref WHERE account_id = :accountId AND ledger_id = :ledgerId",
    )
    suspend fun findAccountLedgerCrossRef(accountId: Long, ledgerId: Long): AccountLedgerCrossRef?

    /** 返回指定账户适用的账本标识。 */
    @Query("SELECT ledger_id FROM account_ledger_cross_ref WHERE account_id = :accountId")
    suspend fun findAccountLedgerIds(accountId: Long): List<Long>

    /** 新增账本。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLedger(ledger: LedgerEntity): Long

    /** 更新账本。 */
    @Update
    suspend fun updateLedgerRow(ledger: LedgerEntity): Int

    /** 删除指定账本。 */
    @Query("DELETE FROM ledgers WHERE id = :ledgerId")
    suspend fun deleteLedgerRow(ledgerId: Long): Int

    /** 返回指定账本下的明细数量。 */
    @Query("SELECT COUNT(*) FROM transactions WHERE ledger_id = :ledgerId")
    suspend fun countTransactionsByLedgerId(ledgerId: Long): Int

    /** 返回指定账本关联的账户数量。 */
    @Query("SELECT COUNT(*) FROM account_ledger_cross_ref WHERE ledger_id = :ledgerId")
    suspend fun countAccountsByLedgerId(ledgerId: Long): Int

    /** 在同一事务中更新账本，并禁止隐藏当前账本。 */
    @Transaction
    suspend fun updateLedgerSafely(ledger: LedgerEntity): LedgerMutationResult {
        val existing = findLedger(ledger.id) ?: return LedgerMutationResult.NOT_FOUND
        val currentLedgerId = findCurrentLedgerId() ?: return LedgerMutationResult.SETTINGS_MISSING
        if (existing.id == currentLedgerId && ledger.isHidden) {
            return LedgerMutationResult.CURRENT_LEDGER
        }
        return if (updateLedgerRow(ledger) > 0) {
            LedgerMutationResult.SUCCESS
        } else {
            LedgerMutationResult.UPDATE_FAILED
        }
    }

    /** 在同一事务中切换账本，并拒绝不存在或隐藏的目标账本。 */
    @Transaction
    suspend fun selectLedgerSafely(ledgerId: Long): LedgerMutationResult {
        val ledger = findLedger(ledgerId) ?: return LedgerMutationResult.NOT_FOUND
        if (ledger.isHidden) return LedgerMutationResult.HIDDEN_LEDGER
        if (findCurrentLedgerId() == null) return LedgerMutationResult.SETTINGS_MISSING
        return if (updateCurrentLedger(ledgerId) > 0) {
            LedgerMutationResult.SUCCESS
        } else {
            LedgerMutationResult.UPDATE_FAILED
        }
    }

    /** 在同一事务中删除账本，并保护当前账本、明细和账户关联。 */
    @Transaction
    suspend fun deleteLedgerSafely(ledgerId: Long): LedgerMutationResult {
        val ledger = findLedger(ledgerId) ?: return LedgerMutationResult.NOT_FOUND
        val currentLedgerId = findCurrentLedgerId() ?: return LedgerMutationResult.SETTINGS_MISSING
        if (ledger.id == currentLedgerId) return LedgerMutationResult.CURRENT_LEDGER
        if (ledger.id == 1L) return LedgerMutationResult.DEFAULT_LEDGER
        if (countTransactionsByLedgerId(ledgerId) > 0) return LedgerMutationResult.HAS_TRANSACTIONS
        if (countAccountsByLedgerId(ledgerId) > 0) return LedgerMutationResult.HAS_ACCOUNT_LINKS
        return if (deleteLedgerRow(ledgerId) > 0) {
            LedgerMutationResult.SUCCESS
        } else {
            LedgerMutationResult.DELETE_FAILED
        }
    }

    /** 插入账户与账本关联。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountLedgerCrossRefs(refs: List<AccountLedgerCrossRef>)

    /** 清除指定账户的账本关联。 */
    @Query("DELETE FROM account_ledger_cross_ref WHERE account_id = :accountId")
    suspend fun deleteAccountLedgerCrossRefs(accountId: Long)

    /** 返回账本当前最大排序值。 */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM ledgers")
    suspend fun maxLedgerSortOrder(): Int

    /** 返回账本数量。 */
    @Query("SELECT COUNT(*) FROM ledgers")
    suspend fun countLedgers(): Int

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

    /** 返回使用指定币种作为本位币的账本数量。 */
    @Query("SELECT COUNT(*) FROM ledgers WHERE base_currency_key = :currencyKey")
    suspend fun countLedgersByBaseCurrencyKey(currencyKey: String): Int

    /** 删除未被账户引用的自定义币种。 */
    @Query("DELETE FROM currencies WHERE `key` = :currencyKey AND is_builtin = 0")
    suspend fun deleteCustomCurrency(currencyKey: String): Int

    /** 在同一事务中校验并删除未被引用的自定义币种。 */
    @Transaction
    suspend fun deleteCustomCurrencySafely(currencyKey: String): CurrencyDeleteResult {
        val currency = findCurrency(currencyKey) ?: return CurrencyDeleteResult.NOT_FOUND
        if (currency.isBuiltin) return CurrencyDeleteResult.BUILTIN_CURRENCY
        if (countAccountsByCurrencyKey(currencyKey) > 0) return CurrencyDeleteResult.ACCOUNT_LINKS
        if (countLedgersByBaseCurrencyKey(currencyKey) > 0) return CurrencyDeleteResult.LEDGER_LINKS
        if (countTransactionsByCurrencyKey(currencyKey) > 0) return CurrencyDeleteResult.TRANSACTION_LINKS
        return if (deleteCustomCurrency(currencyKey) > 0) {
            CurrencyDeleteResult.SUCCESS
        } else {
            CurrencyDeleteResult.DELETE_FAILED
        }
    }

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

    /** 保存账户并整体替换其适用账本。 */
    @Transaction
    suspend fun saveAccountWithLedgers(account: AccountEntity, ledgerIds: Set<Long>): Long {
        val accountId = saveAccount(account)
        deleteAccountLedgerCrossRefs(accountId)
        insertAccountLedgerCrossRefs(ledgerIds.map { AccountLedgerCrossRef(accountId, it) })
        return accountId
    }

    /** 在同一事务中按审计式兑换把账户余额转入目标币种的继任账户。 */
    @Transaction
    suspend fun exchangeAccountCurrency(
        existing: AccountEntity,
        desired: AccountEntity,
        ledgerIds: Set<Long>,
    ): Long {
        val balanceMinor = existing.openingBalanceMinor + transactionsByAccount(existing.id).sumOf { record ->
            when {
                record.type == TransactionType.INCOME -> record.amountMinor
                record.type == TransactionType.TRANSFER &&
                    record.transferDirection == TransferDirection.IN -> record.amountMinor
                else -> -record.amountMinor
            }
        }
        val oldCurrency = findCurrency(existing.currencyKey)
            ?: throw AccountingWriteException("账户币种不存在")
        val newCurrency = findCurrency(desired.currencyKey)
            ?: throw AccountingWriteException("账户币种不存在")
        val ledgerId = findCurrentLedgerId() ?: firstLedgerIdByAccount(existing.id) ?: 1L
        val baseCurrency = findLedger(ledgerId)?.baseCurrencyKey?.let { findCurrency(it) }
            ?: throw AccountingWriteException("账本位币不存在")
        val exchangeId = System.currentTimeMillis()
        val occurredAt = System.currentTimeMillis()
        val newAccountId = insertAccount(
            desired.copy(
                id = 0,
                openingBalanceMinor = 0,
                sortOrder = maxAccountSortOrder() + 1,
                isDefault = desired.isDefault,
                isArchived = false,
            ),
        )
        insertAccountLedgerCrossRefs(ledgerIds.map { AccountLedgerCrossRef(newAccountId, it) })
        insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amountMinor = balanceMinor,
                accountId = existing.id,
                categoryId = null,
                merchant = "",
                note = "币种兑换转出",
                occurredAt = occurredAt,
                source = TransactionSource.MANUAL,
                ledgerId = ledgerId,
                currencyKey = oldCurrency.key,
                baseAmountMinor = convertCurrencyMinor(
                    balanceMinor,
                    oldCurrency.rateToCnyScaled,
                    baseCurrency.rateToCnyScaled,
                ),
                exchangeId = exchangeId,
                transferDirection = TransferDirection.OUT,
            ),
        )
        val newAmountMinor = convertCurrencyMinor(
            balanceMinor,
            oldCurrency.rateToCnyScaled,
            newCurrency.rateToCnyScaled,
        )
        insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amountMinor = newAmountMinor,
                accountId = newAccountId,
                categoryId = null,
                merchant = "",
                note = "币种兑换转入",
                occurredAt = occurredAt,
                source = TransactionSource.MANUAL,
                ledgerId = ledgerId,
                currencyKey = newCurrency.key,
                baseAmountMinor = convertCurrencyMinor(
                    newAmountMinor,
                    newCurrency.rateToCnyScaled,
                    baseCurrency.rateToCnyScaled,
                ),
                exchangeId = exchangeId,
                transferDirection = TransferDirection.IN,
            ),
        )
        markAccountArchived(existing.id)
        ensureDefaultAccount()
        return newAccountId
    }

    /**
     * 标记指定账户为停用并清除其默认状态。
     */
    @Query("UPDATE accounts SET is_archived = 1, is_default = 0 WHERE id = :accountId")
    suspend fun markAccountArchived(accountId: Long)

    /** 删除未被历史账目引用的账户。 */
    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun removeAccount(accountId: Long): Int

    /**
     * 停用指定账户并为剩余有效账户补齐默认项。
     */
    @Transaction
    suspend fun archiveAccount(accountId: Long) {
        markAccountArchived(accountId)
        ensureDefaultAccount()
    }

    /** 删除账户后为剩余有效账户补齐默认项。 */
    @Transaction
    suspend fun deleteAccount(accountId: Long): Int {
        val deleted = removeAccount(accountId)
        if (deleted > 0) ensureDefaultAccount()
        return deleted
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

    /** 返回同方向且排除自身后的重名分类数量。 */
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND type = :type AND id != :categoryId")
    suspend fun countOtherCategoriesByName(categoryId: Long, name: String, type: TransactionType): Int

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

    /** 返回排序最靠前且未隐藏的账本。 */
    @Query("SELECT id FROM ledgers WHERE is_hidden = 0 ORDER BY sort_order, id LIMIT 1")
    suspend fun firstVisibleLedgerId(): Long?

    /** 修复缺失或指向隐藏账本的当前账本设置。 */
    @Transaction
    suspend fun ensureCurrentLedger() {
        val currentLedger = findCurrentLedgerId()?.let { findLedger(it) }
        if (currentLedger == null || currentLedger.isHidden) {
            firstVisibleLedgerId()?.let { updateCurrentLedger(it) }
        }
    }

    /**
     * 在首次启动时建立默认账户与分类。
     */
    @Transaction
    suspend fun seedDefaults() {
        if (countCurrencies() == 0) {
            insertCurrencies(defaultCurrencies())
        }
        if (countLedgers() == 0) {
            insertLedger(
                LedgerEntity(
                    id = 1,
                    name = "日常账本",
                    coverKey = "cover_ocean",
                    useLightText = true,
                    baseCurrencyKey = "cny",
                ),
            )
        }
        if (countAccountTypes() == 0) {
            insertAccountTypes(defaultAccountTypes())
        }
        if (countAccounts() == 0) {
            val accountId = insertAccount(
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
            insertAccountLedgerCrossRefs(listOf(AccountLedgerCrossRef(accountId, 1)))
        }
        ensureDefaultAccount()
        if (countSettings() == 0) {
            upsertSettings(AppSettingsEntity())
        }
        ensureCurrentLedger()
        if (countCategories() == 0) {
            insertCategories(
                listOf(
                    CategoryEntity(name = "消费", type = TransactionType.EXPENSE, sortOrder = 0, iconKey = "custom_consumption"),
                    CategoryEntity(name = "餐饮", type = TransactionType.EXPENSE, sortOrder = 1, iconKey = "custom_dining"),
                    CategoryEntity(name = "其他", type = TransactionType.EXPENSE, sortOrder = 2, iconKey = "custom_other"),
                    CategoryEntity(name = "转账", type = TransactionType.EXPENSE, sortOrder = 3, iconKey = "custom_transfer"),
                    CategoryEntity(name = "教育", type = TransactionType.EXPENSE, sortOrder = 4, iconKey = "custom_education"),
                    CategoryEntity(name = "购物", type = TransactionType.EXPENSE, sortOrder = 5, iconKey = "custom_shopping"),
                    CategoryEntity(name = "人情社交", type = TransactionType.EXPENSE, sortOrder = 6, iconKey = "custom_social"),
                    CategoryEntity(name = "娱乐", type = TransactionType.EXPENSE, sortOrder = 7, iconKey = "custom_entertainment"),
                    CategoryEntity(name = "住房", type = TransactionType.EXPENSE, sortOrder = 8, iconKey = "custom_housing"),
                    CategoryEntity(name = "交通", type = TransactionType.EXPENSE, sortOrder = 9, iconKey = "custom_transport"),
                    CategoryEntity(name = "红包", type = TransactionType.EXPENSE, sortOrder = 10, iconKey = "custom_red_packet"),
                    CategoryEntity(name = "投资", type = TransactionType.EXPENSE, sortOrder = 11, iconKey = "custom_investment"),
                    CategoryEntity(name = "通讯", type = TransactionType.EXPENSE, sortOrder = 12, iconKey = "custom_communication"),
                    CategoryEntity(name = "医疗", type = TransactionType.EXPENSE, sortOrder = 13, iconKey = "custom_medical"),
                    CategoryEntity(name = "旅行", type = TransactionType.EXPENSE, sortOrder = 14, iconKey = "custom_travel"),
                    CategoryEntity(name = "借出", type = TransactionType.EXPENSE, sortOrder = 15, iconKey = "custom_lend_out"),
                    CategoryEntity(name = "还债", type = TransactionType.EXPENSE, sortOrder = 16, iconKey = "custom_repay"),
                    CategoryEntity(name = "美容", type = TransactionType.EXPENSE, sortOrder = 17, iconKey = "custom_beauty"),
                    CategoryEntity(name = "亲子", type = TransactionType.EXPENSE, sortOrder = 18, iconKey = "custom_family"),
                    CategoryEntity(name = "宠物", type = TransactionType.EXPENSE, sortOrder = 19, iconKey = "custom_pet"),
                    CategoryEntity(name = "代付", type = TransactionType.EXPENSE, sortOrder = 20, iconKey = "custom_pay_for"),
                    CategoryEntity(name = "转账", type = TransactionType.INCOME, sortOrder = 0, iconKey = "custom_transfer"),
                    CategoryEntity(name = "退款", type = TransactionType.INCOME, sortOrder = 1, iconKey = "custom_refund"),
                    CategoryEntity(name = "红包", type = TransactionType.INCOME, sortOrder = 2, iconKey = "custom_red_packet"),
                    CategoryEntity(name = "薪资", type = TransactionType.INCOME, sortOrder = 3, iconKey = "custom_salary"),
                    CategoryEntity(name = "理财", type = TransactionType.INCOME, sortOrder = 4, iconKey = "custom_wealth"),
                    CategoryEntity(name = "借入", type = TransactionType.INCOME, sortOrder = 5, iconKey = "custom_borrow_in"),
                    CategoryEntity(name = "收债", type = TransactionType.INCOME, sortOrder = 6, iconKey = "custom_collect"),
                    CategoryEntity(name = "其他", type = TransactionType.INCOME, sortOrder = 7, iconKey = "custom_other"),
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
        LedgerEntity::class,
        AccountLedgerCrossRef::class,
        AccountTypeEntity::class,
        CurrencyEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        AppSettingsEntity::class,
    ],
    version = 15,
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
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
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

        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            /** 建立账本、账户多账本关联与账目归属，并把已有数据迁入默认账本。 */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `ledgers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `cover_key` TEXT NOT NULL,
                        `use_light_text` INTEGER NOT NULL,
                        `base_currency_key` TEXT NOT NULL,
                        `is_hidden` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ledgers_name` ON `ledgers` (`name`)",
                )
                connection.executeMigrationSql(
                    "INSERT INTO ledgers (id, name, cover_key, use_light_text, base_currency_key, is_hidden, sort_order) VALUES (1, '日常账本', 'cover_ocean', 1, 'cny', 0, 0)",
                )
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `account_ledger_cross_ref` (
                        `account_id` INTEGER NOT NULL,
                        `ledger_id` INTEGER NOT NULL,
                        PRIMARY KEY(`account_id`, `ledger_id`),
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`ledger_id`) REFERENCES `ledgers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_account_ledger_cross_ref_ledger_id` ON `account_ledger_cross_ref` (`ledger_id`)",
                )
                connection.executeMigrationSql(
                    "INSERT INTO account_ledger_cross_ref (account_id, ledger_id) SELECT id, 1 FROM accounts",
                )
                connection.executeMigrationSql(
                    "ALTER TABLE transactions ADD COLUMN ledger_id INTEGER NOT NULL DEFAULT 1",
                )
                connection.executeMigrationSql(
                    "ALTER TABLE app_settings ADD COLUMN current_ledger_id INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            /** 为账本本位币增加外键，修复历史上已经失效的本位币引用。 */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `ledgers_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `cover_key` TEXT NOT NULL,
                        `use_light_text` INTEGER NOT NULL,
                        `base_currency_key` TEXT NOT NULL,
                        `is_hidden` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        FOREIGN KEY(`base_currency_key`) REFERENCES `currencies`(`key`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `ledgers_new`
                        (`id`, `name`, `cover_key`, `use_light_text`, `base_currency_key`, `is_hidden`, `sort_order`)
                    SELECT
                        `id`, `name`, `cover_key`, `use_light_text`,
                        CASE WHEN EXISTS (
                            SELECT 1 FROM `currencies` WHERE `currencies`.`key` = `ledgers`.`base_currency_key`
                        ) THEN `base_currency_key` ELSE 'cny' END,
                        `is_hidden`, `sort_order`
                    FROM `ledgers`
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `account_ledger_cross_ref_hold` (
                        `account_id` INTEGER NOT NULL,
                        `ledger_id` INTEGER NOT NULL,
                        PRIMARY KEY(`account_id`, `ledger_id`)
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `account_ledger_cross_ref_hold` (`account_id`, `ledger_id`)
                    SELECT `account_id`, `ledger_id` FROM `account_ledger_cross_ref`
                    """.trimIndent(),
                )
                connection.executeMigrationSql("DROP TABLE `account_ledger_cross_ref`")
                connection.executeMigrationSql("DROP TABLE `ledgers`")
                connection.executeMigrationSql("ALTER TABLE `ledgers_new` RENAME TO `ledgers`")
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `account_ledger_cross_ref` (
                        `account_id` INTEGER NOT NULL,
                        `ledger_id` INTEGER NOT NULL,
                        PRIMARY KEY(`account_id`, `ledger_id`),
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`ledger_id`) REFERENCES `ledgers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `account_ledger_cross_ref` (`account_id`, `ledger_id`)
                    SELECT `account_id`, `ledger_id` FROM `account_ledger_cross_ref_hold`
                    """.trimIndent(),
                )
                connection.executeMigrationSql("DROP TABLE `account_ledger_cross_ref_hold`")
                connection.executeMigrationSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ledgers_name` ON `ledgers` (`name`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_ledgers_base_currency_key` ON `ledgers` (`base_currency_key`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_account_ledger_cross_ref_ledger_id` ON `account_ledger_cross_ref` (`ledger_id`)",
                )
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            /** 为当前账本与账目归属补充数据库外键，修复悬空引用并建立账本时间索引。 */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings_new` (
                        `id` INTEGER NOT NULL,
                        `theme_mode` TEXT NOT NULL,
                        `follow_system_color` INTEGER NOT NULL,
                        `predictive_back_animation_enabled` INTEGER NOT NULL DEFAULT 0,
                        `current_ledger_id` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`current_ledger_id`) REFERENCES `ledgers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `app_settings_new`
                        (`id`, `theme_mode`, `follow_system_color`, `predictive_back_animation_enabled`, `current_ledger_id`)
                    SELECT
                        `id`, `theme_mode`, `follow_system_color`, `predictive_back_animation_enabled`,
                        CASE WHEN EXISTS (
                            SELECT 1 FROM `ledgers` WHERE `ledgers`.`id` = `app_settings`.`current_ledger_id`
                        ) THEN `current_ledger_id` ELSE 1 END
                    FROM `app_settings`
                    """.trimIndent(),
                )
                connection.executeMigrationSql("DROP TABLE `app_settings`")
                connection.executeMigrationSql("ALTER TABLE `app_settings_new` RENAME TO `app_settings`")
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_app_settings_current_ledger_id` ON `app_settings` (`current_ledger_id`)",
                )
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount_minor` INTEGER NOT NULL,
                        `account_id` INTEGER NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `occurred_at` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `ledger_id` INTEGER NOT NULL,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`ledger_id`) REFERENCES `ledgers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `transactions_new`
                        (`id`, `type`, `amount_minor`, `account_id`, `category_id`, `merchant`, `note`, `occurred_at`, `source`, `ledger_id`)
                    SELECT
                        `id`, `type`, `amount_minor`, `account_id`, `category_id`, `merchant`, `note`, `occurred_at`, `source`,
                        CASE WHEN EXISTS (
                            SELECT 1 FROM `ledgers` WHERE `ledgers`.`id` = `transactions`.`ledger_id`
                        ) THEN `ledger_id` ELSE 1 END
                    FROM `transactions`
                    """.trimIndent(),
                )
                connection.executeMigrationSql("DROP TABLE `transactions`")
                connection.executeMigrationSql("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_ledger_id_occurred_at_id` ON `transactions` (`ledger_id`, `occurred_at`, `id`)",
                )
            }
        }

        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            /** 为账目固化币种与本位币金额快照，历史报表不再随后续汇率变化。 */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount_minor` INTEGER NOT NULL,
                        `account_id` INTEGER NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `occurred_at` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `ledger_id` INTEGER NOT NULL,
                        `currency_key` TEXT NOT NULL,
                        `base_amount_minor` INTEGER NOT NULL,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`ledger_id`) REFERENCES `ledgers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`currency_key`) REFERENCES `currencies`(`key`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `transactions_new`
                        (`id`, `type`, `amount_minor`, `account_id`, `category_id`, `merchant`, `note`, `occurred_at`, `source`, `ledger_id`, `currency_key`, `base_amount_minor`)
                    SELECT
                        `transactions`.`id`, `transactions`.`type`, `transactions`.`amount_minor`,
                        `transactions`.`account_id`, `transactions`.`category_id`, `transactions`.`merchant`, `transactions`.`note`, `transactions`.`occurred_at`, `transactions`.`source`, `transactions`.`ledger_id`,
                        COALESCE(`account_currency`.`key`, 'cny'),
                        (`transactions`.`amount_minor` * COALESCE(`account_currency`.`rate_to_cny_scaled`, `base_currency`.`rate_to_cny_scaled`) + `base_currency`.`rate_to_cny_scaled` / 2) / `base_currency`.`rate_to_cny_scaled`
                    FROM `transactions`
                    LEFT JOIN `accounts` ON `accounts`.`id` = `transactions`.`account_id`
                    LEFT JOIN `currencies` AS `account_currency` ON `account_currency`.`key` = `accounts`.`currency_key`
                    INNER JOIN `ledgers` ON `ledgers`.`id` = `transactions`.`ledger_id`
                    INNER JOIN `currencies` AS `base_currency` ON `base_currency`.`key` = `ledgers`.`base_currency_key`
                    """.trimIndent(),
                )
                connection.executeMigrationSql("DROP TABLE `transactions`")
                connection.executeMigrationSql("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_currency_key` ON `transactions` (`currency_key`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_ledger_id_occurred_at_id` ON `transactions` (`ledger_id`, `occurred_at`, `id`)",
                )
            }
        }

        internal val MIGRATION_12_13 = object : Migration(12, 13) {
            /** 为转账流水建立可空分类、兑换分组与出入方向，历史收支分类保持不变。 */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount_minor` INTEGER NOT NULL,
                        `account_id` INTEGER NOT NULL,
                        `category_id` INTEGER,
                        `merchant` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `occurred_at` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `ledger_id` INTEGER NOT NULL,
                        `currency_key` TEXT NOT NULL,
                        `base_amount_minor` INTEGER NOT NULL,
                        `exchange_id` INTEGER,
                        `transfer_direction` TEXT,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`ledger_id`) REFERENCES `ledgers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`currency_key`) REFERENCES `currencies`(`key`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    INSERT INTO `transactions_new`
                        (`id`, `type`, `amount_minor`, `account_id`, `category_id`, `merchant`, `note`, `occurred_at`, `source`, `ledger_id`, `currency_key`, `base_amount_minor`)
                    SELECT
                        `id`, `type`, `amount_minor`, `account_id`, `category_id`, `merchant`, `note`, `occurred_at`, `source`, `ledger_id`, `currency_key`, `base_amount_minor`
                    FROM `transactions`
                    """.trimIndent(),
                )
                connection.executeMigrationSql("DROP TABLE `transactions`")
                connection.executeMigrationSql("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_currency_key` ON `transactions` (`currency_key`)",
                )
                connection.executeMigrationSql(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_ledger_id_occurred_at_id` ON `transactions` (`ledger_id`, `occurred_at`, `id`)",
                )
            }
        }

        internal val MIGRATION_13_14 = object : Migration(13, 14) {
            /**
             * 补全与界面一致的支出分类并把"居住"重命名为"住房"。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "UPDATE categories SET name = '住房', icon_key = 'home' WHERE name = '居住' AND type = 'EXPENSE'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET sort_order = 1 WHERE name = '餐饮' AND type = 'EXPENSE'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET sort_order = 5 WHERE name = '购物' AND type = 'EXPENSE'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET sort_order = 7 WHERE name = '娱乐' AND type = 'EXPENSE'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET sort_order = 8 WHERE name = '住房' AND type = 'EXPENSE'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET sort_order = 9 WHERE name = '交通' AND type = 'EXPENSE'",
                )
                connection.executeMigrationSql(
                    """
                    INSERT OR IGNORE INTO categories (name, type, sort_order, icon_key, is_archived) VALUES
                        ('消费', 'EXPENSE', 0, 'store', 0),
                        ('其他', 'EXPENSE', 2, 'more', 0),
                        ('转账', 'EXPENSE', 3, 'bank_cards', 0),
                        ('教育', 'EXPENSE', 4, 'promotions', 0),
                        ('人情社交', 'EXPENSE', 6, 'community', 0),
                        ('红包', 'EXPENSE', 10, 'favorites_fill', 0),
                        ('投资', 'EXPENSE', 11, 'promotions', 0),
                        ('通讯', 'EXPENSE', 12, 'phone', 0),
                        ('医疗', 'EXPENSE', 13, 'favorites_fill', 0),
                        ('旅行', 'EXPENSE', 14, 'map_album', 0),
                        ('借出', 'EXPENSE', 15, 'send', 0),
                        ('还债', 'EXPENSE', 16, 'import', 0),
                        ('美容', 'EXPENSE', 17, 'favorites', 0),
                        ('亲子', 'EXPENSE', 18, 'favorites_fill', 0),
                        ('宠物', 'EXPENSE', 19, 'favorites', 0),
                        ('代付', 'EXPENSE', 20, 'favorites_fill', 0)
                    """.trimIndent(),
                )
            }
        }

        internal val MIGRATION_14_15 = object : Migration(14, 15) {
            /**
             * 补全收入分类并把默认分类图标切换到统一的自定义图标。
             */
            override fun migrate(connection: SQLiteConnection) {
                connection.executeMigrationSql(
                    "UPDATE categories SET name = '薪资', icon_key = 'custom_salary', sort_order = 3 WHERE name = '工资' AND type = 'INCOME'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET name = '其他', icon_key = 'custom_other', sort_order = 7 WHERE name = '其他收入' AND type = 'INCOME'",
                )
                connection.executeMigrationSql(
                    "UPDATE categories SET is_archived = 1 WHERE name = '奖金' AND type = 'INCOME'",
                )
                connection.executeMigrationSql(
                    """
                    INSERT OR IGNORE INTO categories (name, type, sort_order, icon_key, is_archived) VALUES
                        ('转账', 'INCOME', 0, 'custom_transfer', 0),
                        ('退款', 'INCOME', 1, 'custom_refund', 0),
                        ('红包', 'INCOME', 2, 'custom_red_packet', 0),
                        ('理财', 'INCOME', 4, 'custom_wealth', 0),
                        ('借入', 'INCOME', 5, 'custom_borrow_in', 0),
                        ('收债', 'INCOME', 6, 'custom_collect', 0)
                    """.trimIndent(),
                )
                connection.executeMigrationSql(
                    """
                    UPDATE categories SET icon_key = CASE name
                        WHEN '消费' THEN 'custom_consumption'
                        WHEN '餐饮' THEN 'custom_dining'
                        WHEN '其他' THEN 'custom_other'
                        WHEN '转账' THEN 'custom_transfer'
                        WHEN '教育' THEN 'custom_education'
                        WHEN '购物' THEN 'custom_shopping'
                        WHEN '人情社交' THEN 'custom_social'
                        WHEN '娱乐' THEN 'custom_entertainment'
                        WHEN '住房' THEN 'custom_housing'
                        WHEN '交通' THEN 'custom_transport'
                        WHEN '红包' THEN 'custom_red_packet'
                        WHEN '投资' THEN 'custom_investment'
                        WHEN '通讯' THEN 'custom_communication'
                        WHEN '医疗' THEN 'custom_medical'
                        WHEN '旅行' THEN 'custom_travel'
                        WHEN '借出' THEN 'custom_lend_out'
                        WHEN '还债' THEN 'custom_repay'
                        WHEN '美容' THEN 'custom_beauty'
                        WHEN '亲子' THEN 'custom_family'
                        WHEN '宠物' THEN 'custom_pet'
                        WHEN '代付' THEN 'custom_pay_for'
                        WHEN '退款' THEN 'custom_refund'
                        WHEN '薪资' THEN 'custom_salary'
                        WHEN '理财' THEN 'custom_wealth'
                        WHEN '借入' THEN 'custom_borrow_in'
                        WHEN '收债' THEN 'custom_collect'
                        ELSE icon_key
                    END
                    """.trimIndent(),
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
