package com.vos.accounting.backup

import android.content.Context
import android.net.Uri
import com.vos.accounting.data.AccountEntity
import com.vos.accounting.data.AccountingDao
import com.vos.accounting.data.LedgerEntity
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
    private val coverUriPrefix = "cover_uri:"
    private val coverFilePrefix = "cover_file:"
    private val customIconPrefix = "custom:"
    private val mediaPrefix = "media://"
    private val ledgerCoverKind = "ledger_cover"
    private val accountIconKind = "account_icon"
    private val maxMediaFileBytes = 16L * 1024 * 1024

    /** 导出当前全部数据为加密备份字节。 */
    suspend fun export(password: String): ByteArray = withContext(Dispatchers.IO) {
        val mediaBytes = linkedMapOf<String, ByteArray>()
        val mediaRefs = mutableListOf<BackupMediaRef>()
        val accounts = dao.getAllAccounts().map { account ->
            readAccountIcon(account)?.let { (entry, bytes) ->
                mediaBytes[entry] = bytes
                mediaRefs += BackupMediaRef(entry, accountIconKind, account.id)
                account.copy(iconKey = mediaPrefix + entry)
            } ?: account
        }
        val ledgers = dao.getAllLedgers().map { ledger ->
            readLedgerCover(ledger)?.let { (entry, bytes) ->
                mediaBytes[entry] = bytes
                mediaRefs += BackupMediaRef(entry, ledgerCoverKind, ledger.id)
                ledger.copy(coverKey = mediaPrefix + entry)
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
            transactions = data.transactions,
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

    private fun readLedgerCover(ledger: LedgerEntity): Pair<String, ByteArray>? {
        val bytes = when {
            ledger.coverKey.startsWith(coverFilePrefix) -> runCatching {
                File(ledger.coverKey.removePrefix(coverFilePrefix)).readBytes()
            }.getOrNull()
            ledger.coverKey.startsWith(coverUriPrefix) -> runCatching {
                context.contentResolver.openInputStream(
                    Uri.parse(ledger.coverKey.removePrefix(coverUriPrefix)),
                )?.use { it.readBytes() }
            }.getOrNull()
            else -> null
        }
        return bytes?.takeIf { it.isNotEmpty() && it.size <= maxMediaFileBytes }
            ?.let { "ledger_cover_${ledger.id}.jpg" to it }
    }

    private fun readAccountIcon(account: AccountEntity): Pair<String, ByteArray>? {
        if (!account.iconKey.startsWith(customIconPrefix)) return null
        return runCatching {
            context.contentResolver.openInputStream(
                Uri.parse(account.iconKey.removePrefix(customIconPrefix)),
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
        if (!ledger.coverKey.startsWith(mediaPrefix)) return ledger
        val file = mediaFiles[ledger.coverKey.removePrefix(mediaPrefix)] ?: return ledger
        return ledger.copy(coverKey = coverFilePrefix + file.absolutePath)
    }

    private fun remapAccountIcon(account: AccountEntity, mediaFiles: Map<String, File>): AccountEntity {
        if (!account.iconKey.startsWith(mediaPrefix)) return account
        val file = mediaFiles[account.iconKey.removePrefix(mediaPrefix)] ?: return account
        return account.copy(iconKey = customIconPrefix + Uri.fromFile(file).toString())
    }
}
