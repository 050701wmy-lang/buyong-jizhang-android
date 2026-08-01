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
    @ColumnInfo(name = "opening_balance_minor")
    val openingBalanceMinor: Long,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
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
            categories.icon_key AS category_icon_key
        FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.account_id
        INNER JOIN categories ON categories.id = transactions.category_id
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
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount_minor ELSE 0 END), 0) AS income_minor,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_minor ELSE 0 END), 0) AS expense_minor
        FROM transactions
        """,
    )
    fun observeOverviewTotals(): Flow<OverviewTotals>

    /**
     * 持续观察支出分类汇总。
     */
    @Query(
        """
        SELECT categories.name AS category_name, SUM(transactions.amount_minor) AS amount_minor
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.category_id
        WHERE transactions.type = 'EXPENSE'
        GROUP BY categories.id
        ORDER BY amount_minor DESC
        """,
    )
    fun observeExpenseCategoryTotals(): Flow<List<CategoryTotal>>

    /**
     * 插入一个账户。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: AccountEntity): Long

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
     * 返回账户当前最大的排序值。
     */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM accounts")
    suspend fun maxAccountSortOrder(): Int

    /**
     * 新增或更新账户，并保持至多一个默认账户。
     */
    @Transaction
    suspend fun saveAccount(account: AccountEntity): Long {
        if (account.isDefault) clearDefaultAccounts()
        return if (account.id == 0L) {
            insertAccount(account)
        } else {
            updateAccount(account)
            account.id
        }
    }

    /**
     * 停用指定账户并清除其默认状态。
     */
    @Query("UPDATE accounts SET is_archived = 1, is_default = 0 WHERE id = :accountId")
    suspend fun archiveAccount(accountId: Long)

    /**
     * 插入一组分类。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    /**
     * 插入一个分类并返回其标识。
     */
    @Insert
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
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * 删除指定账目。
     */
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    /**
     * 返回账户数量。
     */
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int

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
        if (countAccounts() == 0) {
            insertAccount(
                AccountEntity(
                    name = "现金",
                    type = AccountType.CASH,
                    openingBalanceMinor = 0,
                    sortOrder = 0,
                    isDefault = true,
                ),
            )
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
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 2,
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
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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
