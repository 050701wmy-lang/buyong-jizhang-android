package com.vos.accounting.auto

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vos.accounting.data.AccountingDatabase
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 验证内置声明式规则、样本隔离和导入安全边界。 */
@RunWith(AndroidJUnit4::class)
class AutoBookkeepingRuleEngineTest {
    private lateinit var context: Context
    private lateinit var testContext: Context
    private lateinit var database: AccountingDatabase
    private lateinit var engine: AutoBookkeepingRuleEngine

    /** 创建独立内存数据库与规则引擎。 */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testContext = InstrumentationRegistry.getInstrumentation().context
        database = Room.inMemoryDatabaseBuilder(context, AccountingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        engine = AutoBookkeepingRuleEngine(context, AccountingRepository(database.accountingDao()))
    }

    /** 关闭每个测试独占的内存数据库。 */
    @After
    fun tearDown() {
        database.close()
    }

    /** 验证微信节点样本可提取支出字段并散列交易号。 */
    @Test
    fun parsesWechatAccessibilityFixture() = runBlocking {
        val capture = engine.parse(
            RuleInput(
                packageName = WECHAT_PACKAGE,
                source = AutoCaptureSource.ACCESSIBILITY,
                occurredAt = 1000,
                textParts = fixture("auto_bookkeeping_samples/wechat/accessibility/payment_success.txt"),
            ),
        )

        assertNotNull(capture)
        assertEquals(TransactionType.EXPENSE, capture?.type)
        assertEquals(2850L, capture?.amountMinor)
        assertEquals("便利蜂", capture?.merchant)
        assertEquals("零钱", capture?.paymentMethodKey)
        assertTrue(capture?.externalKeyHash?.length == 64)
    }

    /** 验证 OCR 样本走相同规则而不是独立硬编码解析器。 */
    @Test
    fun parsesAlipayOcrFixture() = runBlocking {
        val capture = engine.parse(
            RuleInput(
                packageName = ALIPAY_PACKAGE,
                source = AutoCaptureSource.LOCAL_OCR,
                occurredAt = 1000,
                textParts = fixture("auto_bookkeeping_samples/alipay/ocr/payment_success.txt"),
            ),
        )

        assertEquals(1890L, capture?.amountMinor)
        assertEquals("杭州地铁", capture?.merchant)
        assertEquals(AutoCaptureSource.LOCAL_OCR, capture?.source)
    }

    /** 验证非支付页面拒绝生成草稿。 */
    @Test
    fun rejectsNonPaymentFixture() = runBlocking {
        val capture = engine.parse(
            RuleInput(
                packageName = WECHAT_PACKAGE,
                source = AutoCaptureSource.ACCESSIBILITY,
                occurredAt = 1000,
                textParts = fixture("auto_bookkeeping_samples/wechat/accessibility/non_payment.txt"),
            ),
        )

        assertNull(capture)
    }

    /** 验证 RE2/J 拒绝依赖回溯语义的反向引用表达式。 */
    @Test
    fun rejectsUnsupportedBacktrackingRegex() {
        val bytes = """
            {
              "schema_version": 1,
              "pack_id": "test.unsafe",
              "pack_version": 1,
              "rules": [{
                "id": "test.rule",
                "provider": "WECHAT",
                "sources": ["ACCESSIBILITY"],
                "package_names": ["com.tencent.mm"],
                "type": "EXPENSE",
                "fields": {"amount": [{"kind": "REGEX", "pattern": "(a)\\1"}]}
              }]
            }
        """.trimIndent().toByteArray()

        val failed = runCatching { engine.validateImportedRulePack(bytes) }.isFailure

        assertTrue(failed)
    }

    /** 验证导入文件严格限制为 1MiB。 */
    @Test
    fun rejectsOversizedRulePack() {
        val failed = runCatching {
            engine.validateImportedRulePack(ByteArray(MAX_RULE_PACK_BYTES + 1))
        }.isFailure

        assertTrue(failed)
    }

    /** 读取一份按平台、来源和场景隔离的脱敏样本。 */
    private fun fixture(path: String): List<String> = testContext.assets.open(path)
        .bufferedReader()
        .useLines { lines -> lines.filter(String::isNotBlank).toList() }
}
