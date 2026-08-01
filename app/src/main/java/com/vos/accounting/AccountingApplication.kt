package com.vos.accounting

import android.app.Application
import com.vos.accounting.data.AccountingDatabase
import com.vos.accounting.data.AccountingRepository

/**
 * 创建并持有应用进程内的数据依赖。
 */
class AccountingApplication : Application() {
    val repository: AccountingRepository by lazy {
        AccountingRepository(AccountingDatabase.create(this).accountingDao())
    }
}
