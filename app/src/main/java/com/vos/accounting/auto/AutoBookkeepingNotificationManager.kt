package com.vos.accounting.auto

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import com.vos.accounting.R
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.AutoBookkeepingNotificationData
import com.vos.accounting.model.AutoBookkeepingStatus
import com.vos.accounting.model.NotificationPrivacyMode
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import com.xzakota.hyper.notification.focus.FocusNotification
import java.math.BigDecimal

/** 自动账单通知渠道标识。 */
const val AUTO_BOOKKEEPING_CHANNEL_ID = "ai_bookkeeping"

/** 打开自动账单编辑页时携带的事件标识键。 */
const val EXTRA_AUTO_BOOKKEEPING_EVENT_ID = "auto_bookkeeping_event_id"

/** 承担自动账单通知创建、更新和移除。 */
class AutoBookkeepingNotificationManager(
    private val context: Context,
    private val repository: AccountingRepository,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /** 为待确认账单发布符合当前隐私设置的通知。 */
    suspend fun post(eventId: Long) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val data = repository.findAutoBookkeepingNotificationData(eventId) ?: return
        createChannel()
        val settings = repository.getSettingsSnapshot()
        notificationManager.notify(
            notificationId(eventId),
            buildNotification(data, settings.notificationPrivacyMode, settings.xiaomiSuperIslandEnabled),
        )
    }

    /** 移除已经确认或忽略的账单通知。 */
    fun cancel(eventId: Long) {
        notificationManager.cancel(notificationId(eventId))
    }

    /** 创建高重要性账单渠道。 */
    private fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                AUTO_BOOKKEEPING_CHANNEL_ID,
                "AI 记账",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "支付后提醒确认或编辑待入账记录"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    /** 按隐私模式组装可确认、编辑和忽略的通知。 */
    private fun buildNotification(
        data: AutoBookkeepingNotificationData,
        privacyMode: NotificationPrivacyMode,
        xiaomiSuperIslandEnabled: Boolean,
    ): Notification {
        val event = data.event
        val generic = privacyMode == NotificationPrivacyMode.HIDE_DETAILS
        val recorded = event.status == AutoBookkeepingStatus.CONFIRMED
        val title = when {
            recorded && generic -> "检测到一笔已记录账单"
            recorded -> "已记录 · ${notificationTitle(data)}"
            generic -> "检测到一笔待确认账单"
            else -> notificationTitle(data)
        }
        val content = when {
            recorded && generic -> "无需重复入账"
            generic -> "点击查看并编辑"
            else -> notificationText(data)
        }
        val confirmAction = if (!recorded && event.canConfirm) {
            Notification.Action.Builder(
                null,
                "确认入账",
                actionPendingIntent(ACTION_CONFIRM_AUTO_BOOKKEEPING, event.id, 1),
            ).build()
        } else {
            null
        }
        val builder = Notification.Builder(context, AUTO_BOOKKEEPING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setAutoCancel(recorded)
            .setContentTitle(title)
            .setContentText(content)
        if (xiaomiSuperIslandEnabled) {
            builder.addExtras(buildSuperIslandExtras(data, privacyMode, recorded, confirmAction))
        }
        if (!recorded) {
            builder.setContentIntent(editPendingIntent(event.id)).addAction(
                Notification.Action.Builder(
                    null,
                    "编辑",
                    editPendingIntent(event.id),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    "忽略",
                    actionPendingIntent(ACTION_IGNORE_AUTO_BOOKKEEPING, event.id, 2),
                ).build(),
            )
            if (confirmAction != null) {
                builder.addAction(confirmAction)
            }
        }
        when (privacyMode) {
            NotificationPrivacyMode.SHOW_DETAILS -> builder.setVisibility(Notification.VISIBILITY_PUBLIC)
            NotificationPrivacyMode.HIDE_DETAILS -> builder.setVisibility(Notification.VISIBILITY_PRIVATE)
            NotificationPrivacyMode.HIDE_ON_LOCK_SCREEN -> {
                builder.setVisibility(Notification.VISIBILITY_PRIVATE)
                builder.setPublicVersion(
                    Notification.Builder(context, AUTO_BOOKKEEPING_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(if (recorded) "检测到一笔已记录账单" else "检测到一笔待确认账单")
                        .setContentText("解锁后查看详情")
                        .build(),
                )
            }
        }
        return builder.build()
    }

    /** 构建 HyperOS 3 超级岛与 HyperOS 2 焦点通知共用的扩展数据。 */
    private fun buildSuperIslandExtras(
        data: AutoBookkeepingNotificationData,
        privacyMode: NotificationPrivacyMode,
        recorded: Boolean,
        confirmAction: Notification.Action?,
    ) = FocusNotification.buildV3 {
        val showDetails = privacyMode != NotificationPrivacyMode.HIDE_DETAILS
        val status = if (recorded) "已记录" else "待确认"
        val title = if (showDetails) notificationTitle(data) else "账单$status"
        val content = if (showDetails) notificationText(data) else "打开随记查看详情"
        val islandAccount = if (showDetails) data.accountName ?: "待选账户" else "随记账单"
        val islandRight = if (showDetails && !recorded) {
            "${typeLabel(data.event.type)} ${formatAmount(data.event.amountMinor)}"
        } else {
            status
        }
        val appIcon = createPicture(
            "accounting_app_icon",
            Icon.createWithResource(context, R.mipmap.ic_launcher),
        )
        val confirmActionKey = confirmAction?.let {
            createAction("miui.focus.action_confirm_bookkeeping", it)
        }

        business = "bookkeeping"
        isShowNotification = true
        islandFirstFloat = true
        enableFloat = true
        filterWhenNoPermission = false
        ticker = title
        tickerPic = appIcon
        aodTitle = if (privacyMode == NotificationPrivacyMode.SHOW_DETAILS) title else "账单$status"
        aodPic = appIcon

        baseInfo {
            type = 2
            this.title = title
            colorTitle = "#4C82F7"
            colorTitleDark = "#4C82F7"
            this.content = content
        }
        hintInfo {
            type = 1
            this.title = status
            if (confirmActionKey != null) {
                actionInfo {
                    action = confirmActionKey
                }
            } else {
                this.content = if (recorded) "无需重复入账" else "请先补全账单信息"
            }
        }
        island {
            islandProperty = if (confirmActionKey != null) 2 else 1
            highlightColor = "#2B6BF3"
            smallIslandArea {
                picInfo {
                    type = 1
                    pic = appIcon
                    contentDescription = "随记账单"
                }
            }
            bigIslandArea {
                imageTextInfoLeft {
                    type = 1
                    picInfo {
                        type = 1
                        pic = appIcon
                        contentDescription = "随记账单"
                    }
                    textInfo {
                        this.title = islandAccount
                        narrowFont = true
                    }
                }
                imageTextInfoRight {
                    type = 2
                    textInfo {
                        this.title = islandRight
                        showHighlightColor = showDetails
                        narrowFont = true
                    }
                }
            }
        }
    }

    /** 创建点击正文或编辑操作时打开 Activity 的意图。 */
    private fun editPendingIntent(eventId: Long): PendingIntent = PendingIntent.getActivity(
        context,
        notificationId(eventId),
        Intent(context, AutoBookkeepingSheetActivity::class.java)
            .putExtra(EXTRA_AUTO_BOOKKEEPING_EVENT_ID, eventId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** 创建无需打开页面即可确认或忽略的广播意图。 */
    private fun actionPendingIntent(action: String, eventId: Long, offset: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            notificationId(eventId) * 10 + offset,
            Intent(context, AutoBookkeepingActionReceiver::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(EXTRA_AUTO_BOOKKEEPING_EVENT_ID, eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** 返回通知标题中的平台与账单方向。 */
    private fun notificationTitle(data: AutoBookkeepingNotificationData): String =
        "${providerLabel(data.event.provider)}${typeLabel(data.event.type)} ${formatAmount(data.event.amountMinor)}"

    /** 返回通知正文中的商户、分类和账户摘要。 */
    private fun notificationText(data: AutoBookkeepingNotificationData): String = listOfNotNull(
        data.event.merchant.ifBlank { null },
        data.categoryName,
        data.accountName,
    ).joinToString(" · ").ifBlank { "点击查看并编辑" }

    /** 将长整型最小货币单位格式化为人民币金额。 */
    private fun formatAmount(amountMinor: Long): String =
        "¥${BigDecimal.valueOf(amountMinor, 2).toPlainString()}"

    /** 生成稳定且位于正整数范围内的通知编号。 */
    private fun notificationId(eventId: Long): Int = (eventId % Int.MAX_VALUE).toInt().coerceAtLeast(1)
}

/** 返回支付平台的中文短名称。 */
private fun providerLabel(provider: PaymentProvider): String = when (provider) {
    PaymentProvider.WECHAT -> "微信"
    PaymentProvider.ALIPAY -> "支付宝"
    PaymentProvider.UNIONPAY -> "云闪付"
}

/** 返回账单方向的中文短名称。 */
private fun typeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
}
