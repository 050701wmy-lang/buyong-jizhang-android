package com.vos.accounting.backup

import android.content.Context
import android.net.Uri
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountingDao
import com.vos.accounting.data.LEDGER_COVER_URI_PREFIX
import com.vos.accounting.data.LEDGER_COVER_FILE_PREFIX
import com.vos.accounting.data.ACCOUNT_ICON_CUSTOM_PREFIX
import com.vos.accounting.data.BACKUP_MEDIA_PREFIX
import com.vos.accounting.data.LedgerEntity
import com.vos.accounting.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 编排备份导出、解析校验与恢复应用。
 */
class BackupManager(
    private val context: Context,
    private val dao: AccountingDao,
) {
    private val maxMediaFileBytes = 16L * 1024 * 1024

    /** 导出当前全部数据为加密备份字节。 */
    suspend fun export(password: String): ByteArray = withContext(Dispatchers.IO) {
        val mediaBytes = linkedMapOf<String, ByteArray>()
        val mediaRefs = mutableListOf<BackupMediaRef>()
        val accounts = dao.getAllAccounts().map { account ->
            readAccountIcon(account)?.let { (entry, bytes) ->
                mediaBytes[entry] = bytes
                mediaRefs += BackupMediaRef(entry)
                account.copy(iconKey = BACKUP_MEDIA_PREFIX + entry)
            } ?: account
        }
        val ledgers = dao.getAllLedgers().map { ledger ->
            readLedgerCover(ledger)?.let { (entry, bytes) ->
                mediaBytes[entry] = bytes
                mediaRefs += BackupMediaRef(entry)
                ledger.copy(coverKey = BACKUP_MEDIA_PREFIX + entry)
            } ?: ledger
        }
        val data = BackupData(
            createdAt = System.currentTimeMillis(),
            accounts = accounts,
            ledgers = ledgers,
            accountLedgerCrossRefs = dao.getAllAccountLedgerCrossRefs(),
            accountTypes = dao.getAllAccountTypes(),
            currencies = dao.getAllCurrencies(),
            categories = dao.getAllCategories(),
            transactions = dao.getAllTransactions(),
            autoBookkeepingEvents = dao.getAllAutoBookkeepingEvents(),
            autoCategoryMappings = dao.getAllAutoCategoryMappings(),
            autoAccountMappings = dao.getAllAutoAccountMappings(),
            settings = dao.getSettings(),
            media = mediaRefs,
        )
        val payload = BackupCodec.buildZip(data, mediaBytes)
        BackupCodec.encrypt(payload, password)
    }

    /** 解密并校验备份文件，返回待恢复内容。 */
    suspend fun parse(blob: ByteArray, password: String): PreparedBackup = withContext(Dispatchers.IO) {
        val content = BackupCodec.unzip(BackupCodec.decrypt(blob, password))
        validate(content.data, content.media)
        PreparedBackup(
            summary = BackupSummary(
                createdAt = content.data.createdAt,
                accountCount = content.data.accounts.size,
                ledgerCount = content.data.ledgers.size,
                transactionCount = content.data.transactions.size,
                currencyCount = content.data.currencies.size,
                mediaCount = content.data.media.size,
            ),
            data = content.data,
            mediaBytes = content.media,
        )
    }

    /** 恢复备份：写入媒体并全量替换数据库。 */
    suspend fun apply(prepared: PreparedBackup): Unit = withContext(Dispatchers.IO) {
        val data = prepared.data
        val mediaFiles = writeMedia(prepared.mediaBytes)
        dao.replaceAll(
            currencies = data.currencies,
            accountTypes = data.accountTypes,
            ledgers = data.ledgers.map { remapLedgerCover(it, mediaFiles) },
            accounts = data.accounts.map { remapAccountIcon(it, mediaFiles) },
            categories = data.categories,
            crossRefs = data.accountLedgerCrossRefs,
            transactions = restoreTransactionSnapshots(data.transactions, data.ledgers),
            autoBookkeepingEvents = data.autoBookkeepingEvents,
            autoCategoryMappings = data.autoCategoryMappings,
            autoAccountMappings = data.autoAccountMappings,
            settings = data.settings,
        )
    }

    /** 校验备份内部引用与大小限制。 */
    private fun validate(data: BackupData, mediaBytes: Map<String, ByteArray>) {
        val referenced = data.media.map(BackupMediaRef::entryName).toSet()
        val missing = referenced - mediaBytes.keys
        if (missing.isNotEmpty()) throw BackupException("备份媒体文件缺失")
        val total = mediaBytes.values.sumOf { it.size.toLong() }
        if (total > BackupCodec.MAX_TOTAL_MEDIA_BYTES) throw BackupException("备份媒体文件过大")
    }

    /** 为旧版备份中尚未保存历史本位币标识的账目补充当时可知的账本本位币。 */
    private fun restoreTransactionSnapshots(
        transactions: List<TransactionEntity>,
        ledgers: List<LedgerEntity>,
    ): List<TransactionEntity> {
        val ledgerCurrencies = ledgers.associate { it.id to it.baseCurrencyKey }
        return transactions.map { transaction ->
            transaction.copy(
                accountAmountMinor = transaction.accountAmountMinor.takeIf { it > 0 } ?: transaction.amountMinor,
                baseCurrencyKey = transaction.baseCurrencyKey.ifEmpty {
                    ledgerCurrencies.getValue(transaction.ledgerId)
                },
            )
        }
    }

    private fun readLedgerCover(ledger: LedgerEntity): Pair<String, ByteArray>? {
        val bytes = when {
            ledger.coverKey.startsWith(LEDGER_COVER_FILE_PREFIX) -> runCatching {
                File(ledger.coverKey.removePrefix(LEDGER_COVER_FILE_PREFIX)).readBytes()
            }.getOrNull()
            ledger.coverKey.startsWith(LEDGER_COVER_URI_PREFIX) -> runCatching {
                context.contentResolver.openInputStream(
                    Uri.parse(ledger.coverKey.removePrefix(LEDGER_COVER_URI_PREFIX)),
                )?.use { it.readBytes() }
            }.getOrNull()
            else -> null
        }
        return bytes?.takeIf { it.isNotEmpty() && it.size <= maxMediaFileBytes }
            ?.let { "ledger_cover_${ledger.id}.jpg" to it }
    }

    private fun readAccountIcon(account: AccountEntity): Pair<String, ByteArray>? {
        if (!account.iconKey.startsWith(ACCOUNT_ICON_CUSTOM_PREFIX)) return null
        return runCatching {
            context.contentResolver.openInputStream(
                Uri.parse(account.iconKey.removePrefix(ACCOUNT_ICON_CUSTOM_PREFIX)),
            )?.use { it.readBytes() }
        }.getOrNull()?.takeIf { it.isNotEmpty() && it.size <= maxMediaFileBytes }
            ?.let { "account_icon_${account.id}.jpg" to it }
    }

    private fun writeMedia(mediaBytes: Map<String, ByteArray>): Map<String, File> {
        val dir = File(context.filesDir, "backup_media").apply { mkdirs() }
        return mediaBytes.mapValues { (entry, bytes) ->
            File(dir, entry).apply { writeBytes(bytes) }
        }
    }

    private fun remapLedgerCover(ledger: LedgerEntity, mediaFiles: Map<String, File>): LedgerEntity {
        if (!ledger.coverKey.startsWith(BACKUP_MEDIA_PREFIX)) return ledger
        val file = mediaFiles[ledger.coverKey.removePrefix(BACKUP_MEDIA_PREFIX)] ?: return ledger
        return ledger.copy(coverKey = LEDGER_COVER_FILE_PREFIX + file.absolutePath)
    }

    private fun remapAccountIcon(account: AccountEntity, mediaFiles: Map<String, File>): AccountEntity {
        if (!account.iconKey.startsWith(BACKUP_MEDIA_PREFIX)) return account
        val file = mediaFiles[account.iconKey.removePrefix(BACKUP_MEDIA_PREFIX)] ?: return account
        return account.copy(iconKey = ACCOUNT_ICON_CUSTOM_PREFIX + Uri.fromFile(file).toString())
    }
}
