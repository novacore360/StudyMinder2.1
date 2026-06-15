package com.studyminder.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.studyminder.R
import com.studyminder.data.model.RankSystem
import com.studyminder.data.model.ScheduleStatus
import com.studyminder.data.repository.StudyRepository
import com.studyminder.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "studyminder_channel"
        const val CHANNEL_REMINDER_ID = "studyminder_reminder"
        const val CHANNEL_RANK_ID = "studyminder_rank"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val ACTION_ALARM = "com.studyminder.ALARM_ACTION"
        const val ACTION_REPEAT = "com.studyminder.REPEAT_ALARM"

        fun scheduleAlarm(context: Context, scheduleId: String, triggerAtMillis: Long, repeat: Boolean = false) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = if (repeat) ACTION_REPEAT else ACTION_ALARM
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            }
            val requestCode = scheduleId.hashCode() + if (repeat) 100000 else 0
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        fun cancelAlarm(context: Context, scheduleId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            listOf(0, 100000).forEach { offset ->
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, scheduleId.hashCode() + offset, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID, "StudyMinder Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Academic schedule reminders"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 600)
                    setSound(soundUri, audioAttr)
                    enableLights(true)
                }
                nm.createNotificationChannel(channel)

                val reminderChannel = NotificationChannel(
                    CHANNEL_REMINDER_ID, "StudyMinder Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Ongoing reminders every 5 minutes"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                }
                nm.createNotificationChannel(reminderChannel)

                // Rank-up channel — high importance so it heads-up on the lock screen
                val rankChannel = NotificationChannel(
                    CHANNEL_RANK_ID, "Rank Up Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies you when you reach a new rank"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 500)
                    enableLights(true)
                }
                nm.createNotificationChannel(rankChannel)
            }
        }

        /**
         * Checks whether the user has crossed any new rank threshold and, if so,
         * fires a system notification. Safe to call from a BroadcastReceiver
         * (i.e. works even when the app is not running).
         */
        fun checkAndFireRankNotification(context: Context) {
            val repo = StudyRepository.getInstance(context)
            val totalPoints = repo.getTotalDonePoints()
            val notified = repo.getNotifiedRanks()

            for (rank in RankSystem.ranks) {
                if (totalPoints >= rank.requiredPoints && !notified.contains(rank.name)) {
                    // Mark first so duplicate broadcasts can't fire it twice
                    repo.markRankNotified(rank.name)

                    // Tapping the notification opens the app on the Rank tab
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("go_to_rank", true)
                    }
                    val tapPendingIntent = PendingIntent.getActivity(
                        context,
                        rank.name.hashCode(),
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notification = NotificationCompat.Builder(context, CHANNEL_RANK_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("🏆 New Rank Unlocked: ${rank.name}!")
                        .setContentText("\"${rank.quote}\"")
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(
                                    "You've reached the rank of ${rank.name}!\n\n" +
                                    "\"${rank.quote}\"\n\nTap to see your new badge."
                                )
                        )
                        .setColor(0xFFFFD700.toInt())
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(longArrayOf(0, 200, 100, 200, 100, 500))
                        .setAutoCancel(true)
                        .setContentIntent(tapPendingIntent)
                        .build()

                    nm.notify("rank_${rank.name}".hashCode(), notification)
                    break // one rank-up notification at a time
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAll(context)
            ACTION_ALARM, ACTION_REPEAT -> {
                val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
                handleAlarm(context, scheduleId, intent.action == ACTION_REPEAT)
            }
        }
    }

    private fun handleAlarm(context: Context, scheduleId: String, isRepeat: Boolean) {
        val repo = StudyRepository.getInstance(context)
        val schedule = repo.getScheduleById(scheduleId) ?: return
        if (schedule.status != ScheduleStatus.UPCOMING) return

        val now = System.currentTimeMillis()

        // If past deadline and not acknowledged → mark missed
        if (now > schedule.dateTimeMillis && !schedule.isAcknowledged) {
            if (isRepeat) {
                repo.markScheduleMissed(scheduleId)
                cancelAlarm(context, scheduleId)
                showMissedNotification(context, scheduleId)
                return
            }
        }

        showReminderNotification(context, schedule, isRepeat)

        // Schedule repeat every 5 minutes if not acknowledged and before deadline
        if (!schedule.isAcknowledged && now < schedule.dateTimeMillis + 60_000) {
            scheduleAlarm(context, scheduleId, now + 5 * 60 * 1000L, repeat = true)
        }
    }

    private fun showReminderNotification(context: Context, schedule: com.studyminder.data.model.Schedule, isRepeat: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date(schedule.dateTimeMillis))

        val acknowledgeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.studyminder.ACKNOWLEDGE"
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        }
        val ackPending = PendingIntent.getBroadcast(
            context, schedule.id.hashCode() + 200000,
            acknowledgeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.studyminder.MARK_DONE"
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        }
        val donePending = PendingIntent.getBroadcast(
            context, schedule.id.hashCode() + 300000,
            doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutesLeft = ((schedule.dateTimeMillis - System.currentTimeMillis()) / 60000).toInt()
        val timeLeftStr = when {
            minutesLeft <= 0 -> "Due now!"
            minutesLeft < 60 -> "$minutesLeft min left"
            else -> "${minutesLeft / 60}h ${minutesLeft % 60}m left"
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${schedule.type.emoji} ${schedule.type.label} Reminder")
            .setContentText("${schedule.subjectName} — $timeLeftStr")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("📘 ${schedule.subjectName}\n👨‍🏫 ${schedule.professorName}\n🏫 ${schedule.room}\n⏰ $timeStr\n\n${if (schedule.description.isNotBlank()) schedule.description else schedule.title}")
            )
            .setColor(0xFF3E6343.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 300, 200, 300, 200, 600))
            .setSound(soundUri)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_check, "✓ Acknowledge", ackPending)
            .addAction(R.drawable.ic_done, "✅ Mark Done", donePending)
            .build()

        nm.notify(schedule.id.hashCode(), notification)
    }

    private fun showMissedNotification(context: Context, scheduleId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val repo = StudyRepository.getInstance(context)
        val schedule = repo.getScheduleById(scheduleId) ?: return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ Missed: ${schedule.type.label}")
            .setContentText("${schedule.subjectName} was marked as missed")
            .setColor(0xFFE53935.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(schedule.id.hashCode() + 500000, notification)
    }

    private fun rescheduleAll(context: Context) {
        val repo = StudyRepository.getInstance(context)
        val now = System.currentTimeMillis()
        repo.getUpcomingSchedules().forEach { schedule ->
            val triggerTime = schedule.dateTimeMillis - (schedule.remindBeforeMinutes * 60 * 1000L)
            if (triggerTime > now) {
                scheduleAlarm(context, schedule.id, triggerTime)
            }
        }
    }
}
