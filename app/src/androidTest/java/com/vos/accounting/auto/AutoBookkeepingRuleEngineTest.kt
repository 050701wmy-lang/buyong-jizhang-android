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
import java.time.LocalDateTime
import java.time.ZoneId
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
    private lateinit var repository: AccountingRepository
    private lateinit var engine: AutoBookkeepingRuleEngine

    /** 创建独立内存数据库与规则引擎。 */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testContext = InstrumentationRegistry.getInstrumentation().context
        database = Room.inMemoryDatabaseBuilder(context, AccountingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AccountingRepository(database.accountingDao())
        engine = AutoBookkeepingRuleEngine(context, repository)
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

    /** 验证支付宝账单详情可提取顶部对象、真实支付时间和付款方式。 */
    @Test
    fun parsesAlipayAccessibilityBillDetail() = runBlocking {
        val capture = engine.parse(
            RuleInput(
                packageName = ALIPAY_PACKAGE,
                source = AutoCaptureSource.ACCESSIBILITY,
                occurredAt = 1000,
                textParts = fixture("auto_bookkeeping_samples/alipay/accessibility/bill_detail.txt"),
            ),
        )
        val expectedTime = LocalDateTime.parse("2026-08-08T18:54:59")
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(951L, capture?.amountMinor)
        assertEquals("万红羊蛙蛙（商贸西门店）", capture?.merchant)
        assertEquals("花呗", capture?.paymentMethodKey)
        assertEquals(expectedTime, capture?.occurredAt)
        assertTrue(capture?.externalKeyHash?.length == 64)
    }

    /** 验证支付宝账单列表即使含交易成功文本也不会产生草稿。 */
    @Test
    fun rejectsAlipayBillList() = runBlocking {
        val capture = engine.parse(
            RuleInput(
                packageName = ALIPAY_PACKAGE,
                source = AutoCaptureSource.ACCESSIBILITY,
                occurredAt = 1000,
                textParts = fixture("auto_bookkeeping_samples/alipay/accessibility/bill_list.txt"),
            ),
        )

        assertNull(capture)
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

    /** 验证管理页面可读取内置规则摘要。 */
    @Test
    fun readsBuiltinRulePackForManagement() {
        val pack = engine.readBuiltinRulePack()

        assertEquals("builtin.cn.payment", pack.packId)
        assertEquals(7, pack.rules.size)
    }

    /** 验证用户规则包可停用后重新启用。 */
    @Test
    fun togglesImportedRulePack() = runBlocking {
        val bytes = """
            {
              "schema_version": 1,
              "pack_id": "test.manage",
              "pack_version": 1,
              "rules": [{
                "id": "test.manage.rule",
                "provider": "WECHAT",
                "sources": ["ACCESSIBILITY"],
                "package_names": ["com.tencent.mm"],
                "type": "EXPENSE",
                "fields": {"amount": [{"kind": "REGEX", "pattern": "([0-9]+)"}]}
              }]
            }
        """.trimIndent().toByteArray()
        val entity = engine.importRulePack(bytes)

        engine.updateRulePackActive(entity, false)
        assertTrue(repository.getActiveAutoRulePacks().isEmpty())

        engine.updateRulePackActive(entity, true)
        assertEquals(listOf("test.manage"), repository.getActiveAutoRulePacks().map { it.packId })
    }

    /** 读取一份按平台、来源和场景隔离的脱敏样本。 */
    private fun fixture(path: String): List<String> = testContext.assets.open(path)
        .bufferedReader()
        .useLines { lines -> lines.filter(String::isNotBlank).toList() }
}
