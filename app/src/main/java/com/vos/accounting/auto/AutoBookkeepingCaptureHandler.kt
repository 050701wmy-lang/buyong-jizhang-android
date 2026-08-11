package com.vos.accounting.auto

import android.content.Context
import android.graphics.Bitmap
import com.vos.accounting.AccountingApplication
import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.model.AutoCaptureSource
import java.text.Normalizer
import java.util.Locale

/** 汇合无障碍与通知来源，共用同一去重、落草稿和通知流程。 */
class AutoBookkeepingCaptureHandler(context: Context) {
    private val application = context.applicationContext as AccountingApplication
    private val notificationManager = AutoBookkeepingNotificationManager(context, application.repository)
    private val ruleEngine = AutoBookkeepingRuleEngine(context.applicationContext, application.repository)
    private val aiClient = PrivateAutoBookkeepingAiClient(
        application.repository,
        AutoAiCredentialStore(application.repository),
    )

    /** 使用声明式规则和可选私有 AI 解析临时文本，未匹配时不产生空草稿。 */
    suspend fun handle(input: RuleInput): Boolean {
        val deterministic = ruleEngine.parse(input)
        val shouldUseAi = deterministic == null || deterministic.merchant.isBlank() ||
            deterministic.paymentMethodKey.isBlank()
        val aiCapture = if (shouldUseAi) {
            paymentProviderForPackage(input.packageName)?.let { provider ->
                aiClient.parse(provider, input.occurredAt, input.textParts)
            }
        } else {
            null
        }
        val capture = mergeAiCapture(deterministic, aiCapture) ?: return false
        handle(capture)
        return true
    }

    /** 仅执行声明式规则，供 OCR 前置顺序判断使用。 */
    suspend fun parseRules(input: RuleInput): AutoBookkeepingCapture? = ruleEngine.parse(input)

    /** 在 OCR 之后补充可选私有 AI，再进入唯一草稿保存入口。 */
    suspend fun handleParsed(
        input: RuleInput,
        capture: AutoBookkeepingCapture?,
        bitmap: Bitmap? = null,
    ): Boolean {
        val shouldUseAi = capture == null || capture.merchant.isBlank() || capture.paymentMethodKey.isBlank()
        val aiCapture = if (shouldUseAi) {
            paymentProviderForPackage(input.packageName)?.let { provider ->
                aiClient.parse(provider, input.occurredAt, input.textParts, bitmap)
            }
        } else {
            null
        }
        val merged = mergeAiCapture(capture, aiCapture) ?: return false
        handle(merged)
        return true
    }

    /** 保存结构化采集结果，并在生成或合并待确认事件后刷新通知。 */
    suspend fun handle(capture: AutoBookkeepingCapture) {
        val event = application.repository.captureAutoBookkeeping(capture) ?: return
        notificationManager.post(event.id)
    }
}

/** 让 AI 只填充确定性结果的空字段，并把任何冲突降为必须编辑。 */
private fun mergeAiCapture(
    deterministic: AutoBookkeepingCapture?,
    ai: AutoBookkeepingCapture?,
): AutoBookkeepingCapture? {
    if (deterministic == null) return ai
    if (ai == null) return deterministic
    val hasConflict = deterministic.type != ai.type ||
        deterministic.amountMinor != ai.amountMinor ||
        deterministic.currencyKey != ai.currencyKey ||
        (deterministic.merchant.isNotBlank() && ai.merchant.isNotBlank() &&
            normalizeEvidenceText(deterministic.merchant) != normalizeEvidenceText(ai.merchant)) ||
        (deterministic.paymentMethodKey.isNotBlank() && ai.paymentMethodKey.isNotBlank() &&
            normalizeEvidenceText(deterministic.paymentMethodKey) != normalizeEvidenceText(ai.paymentMethodKey))
    val merchantFromAi = deterministic.merchant.isBlank() && ai.merchant.isNotBlank()
    val paymentFromAi = deterministic.paymentMethodKey.isBlank() && ai.paymentMethodKey.isNotBlank()
    val provenance = deterministic.fieldProvenance.toMutableMap().apply {
        if (merchantFromAi) put("merchant", AutoCaptureSource.CLOUD_AI)
        if (paymentFromAi) put("payment_method", AutoCaptureSource.CLOUD_AI)
    }
    return deterministic.copy(
        merchant = deterministic.merchant.ifBlank { ai.merchant },
        paymentMethodKey = deterministic.paymentMethodKey.ifBlank { ai.paymentMethodKey },
        fieldProvenance = provenance,
        hasConflict = deterministic.hasConflict || hasConflict,
        aiAssisted = true,
    )
}

/** 统一全半角、大小写和空白，避免同义 AI 文本被误判成冲突。 */
private fun normalizeEvidenceText(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .filter(Char::isLetterOrDigit)
