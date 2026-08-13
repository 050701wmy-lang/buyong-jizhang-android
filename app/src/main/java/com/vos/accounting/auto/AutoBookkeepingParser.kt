package com.vos.accounting.auto

import com.vos.accounting.model.PaymentProvider
import java.security.MessageDigest

/** 微信 Android 客户端包名。 */
const val WECHAT_PACKAGE = "com.tencent.mm"

/** 支付宝 Android 客户端包名。 */
const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"

/** 云闪付 Android 客户端包名。 */
const val UNIONPAY_PACKAGE = "com.unionpay"

/** 把受支持的支付客户端包名映射为平台。 */
fun paymentProviderForPackage(packageName: String): PaymentProvider? = when (packageName) {
    WECHAT_PACKAGE -> PaymentProvider.WECHAT
    ALIPAY_PACKAGE -> PaymentProvider.ALIPAY
    UNIONPAY_PACKAGE -> PaymentProvider.UNIONPAY
    else -> null
}

/** 将外部交易标识与平台一起散列，避免保存可还原的订单号。 */
internal fun hashExternalKey(provider: PaymentProvider, value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("${provider.name}|${value.trim()}".toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
