package com.vos.accounting.model

import kotlinx.serialization.Serializable

/** 表示自动记账首版支持的支付平台。 */
@Serializable
enum class PaymentProvider {
    WECHAT,
    ALIPAY,
    UNIONPAY,
}

/** 表示自动采集账单的系统入口。 */
@Serializable
enum class AutoCaptureSource {
    ACCESSIBILITY,
    NOTIFICATION,
    LOCAL_OCR,
    ROOT_OCR,
    XPOSED,
    CLOUD_AI,
}

/** 表示自动账单从待确认到处理完成的状态。 */
@Serializable
enum class AutoBookkeepingStatus {
    PENDING,
    CONFIRMED,
    IGNORED,
}

/** 表示账单通知在锁屏上的明细展示策略。 */
@Serializable
enum class NotificationPrivacyMode {
    HIDE_ON_LOCK_SCREEN,
    SHOW_DETAILS,
    HIDE_DETAILS,
}

/** 承载支付页面或通知中已经最小化提取的账单字段。 */
data class AutoBookkeepingCapture(
    val provider: PaymentProvider,
    val source: AutoCaptureSource,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyKey: String = "cny",
    val merchant: String,
    val note: String = "",
    val occurredAt: Long,
    val paymentMethodKey: String = "",
    val externalKeyHash: String? = null,
    val ruleId: String? = null,
    val rulePackVersion: Int? = null,
    val fieldProvenance: Map<String, AutoCaptureSource> = emptyMap(),
    val hasConflict: Boolean = false,
    val aiAssisted: Boolean = false,
)
