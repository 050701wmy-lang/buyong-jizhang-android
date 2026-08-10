package com.vos.accounting

import android.app.Application
import com.vos.accounting.backup.BackupManager
import com.vos.accounting.data.AccountingDatabase
import com.vos.accounting.data.AccountingRepository

/**
 * 创建并持有应用进程内的数据依赖。
 */
class AccountingApplication : Application() {
    private val database: AccountingDatabase by lazy {
        AccountingDatabase.create(this)
    }

    val repository: AccountingRepository by lazy {
        AccountingRepository(database.accountingDao())
    }

    val backupManager: BackupManager by lazy {
        BackupManager(this, database.accountingDao())
    }
}
