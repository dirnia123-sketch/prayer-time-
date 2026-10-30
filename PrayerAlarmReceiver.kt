package com.douo.prayer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

const val EXTRA_PRAYER_NAME = "prayer_name"
private const val CHANNEL_ID = "prayer_times"

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.prayer_notification_title, name))
            .setContentText(context.getString(R.string.prayer_notification_text))
            .setAutoCancel(true)
            .build()
        nm.notify(name.hashCode(), notification)
    }

    companion object {
        /** Schedules one exact alarm per upcoming prayer. */
        fun scheduleAll(context: Context, prayers: List<PrayerItem>) {
            val am = context.getSystemService(AlarmManager::class.java)
            prayers.filter { it.key != "sunrise" }.forEachIndexed { index, p ->
                val intent = Intent(context, PrayerAlarmReceiver::class.java)
                    .putExtra(EXTRA_PRAYER_NAME, p.name)
                val pi = PendingIntent.getBroadcast(
                    context, index, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.time.time, pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.time.time, pi)
                }
            }
        }
    }
}
