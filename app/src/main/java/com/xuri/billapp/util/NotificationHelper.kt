package com.xuri.billapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xuri.billapp.MainActivity
import com.xuri.billapp.R

/**
 * 预算预警通知工具类
 * 当消费超过预算的 80% 时发送提醒通知
 * 超过 100% 时发送超支通知
 */
object NotificationHelper {

    private const val CHANNEL_ID = "budget_alert_channel"
    private const val CHANNEL_NAME = "预算预警"
    private const val CHANNEL_DESC = "当消费接近或超过每日预算时发送通知"

    // 通知 ID
    const val NOTIFICATION_80_PERCENT = 1
    const val NOTIFICATION_100_PERCENT = 2

    /**
     * 创建通知渠道（Android 8.0+ 需要）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 发送预算 80% 预警通知
     */
    fun sendBudget80PercentNotification(context: Context, spent: Double, budget: Double) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("预算预警")
            .setContentText("今日已消费 ¥${String.format("%.2f", spent)}，已达预算 ¥${String.format("%.2f", budget)} 的 80%，请节约开支！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_80_PERCENT, notification)
    }

    /**
     * 发送预算超支通知
     */
    fun sendBudgetExceededNotification(context: Context, spent: Double, budget: Double) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("预算超支")
            .setContentText("今日已消费 ¥${String.format("%.2f", spent)}，已超过预算 ¥${String.format("%.2f", budget)}，请注意控制开支！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_100_PERCENT, notification)
    }
}
