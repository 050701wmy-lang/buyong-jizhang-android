package com.vos.accounting.backup

import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountLedgerCrossRef
import com.vos.accounting.data.AccountTypeEntity
import com.vos.accounting.data.AppSettingsEntity
import com.vos.accounting.data.CategoryEntity
import com.vos.accounting.data.CurrencyEntity
import com.vos.accounting.data.LedgerEntity
import com.vos.accounting.data.TransactionEntity
import kotlinx.serialization.Serializable

/** 备份中媒体资源在压缩包内的标识。 */
@Serializable
data class BackupMediaRef(
    val entryName: String,
    val kind: String,
    val targetId: Long,
)

/** 版本化的全量备份数据，覆盖全部八张持久化表与媒体引用。 */
@Serializable
data class BackupData(
    val formatVersion: Int = 1,
    val createdAt: Long,
    val accounts: List<AccountEntity> = emptyList(),
    val ledgers: List<LedgerEntity> = emptyList(),
    val accountLedgerCrossRefs: List<AccountLedgerCrossRef> = emptyList(),
    val accountTypes: List<AccountTypeEntity> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val settings: AppSettingsEntity? = null,
    val media: List<BackupMediaRef> = emptyList(),
)

/** 备份预览摘要，供恢复前展示。 */
data class BackupSummary(
    val createdAt: Long,
    val accountCount: Int,
    val ledgerCount: Int,
    val transactionCount: Int,
    val currencyCount: Int,
    val mediaCount: Int,
)

/** 解析并校验通过的待恢复备份。 */
class PreparedBackup internal constructor(
    val summary: BackupSummary,
    internal val data: BackupData,
    internal val mediaBytes: Map<String, ByteArray>,
)
