package com.studyminder.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyminder.data.repository.StudyRepository

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULE_ID) ?: return
        val repo = StudyRepository.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            "com.studyminder.ACKNOWLEDGE" -> {
                repo.acknowledgeSchedule(scheduleId)
                AlarmReceiver.cancelAlarm(context, scheduleId)
                nm.cancel(scheduleId.hashCode())
            }
            "com.studyminder.MARK_DONE" -> {
                repo.markScheduleDone(scheduleId)
                AlarmReceiver.cancelAlarm(context, scheduleId)
                nm.cancel(scheduleId.hashCode())

                // Check if marking this schedule done pushed the user into a new rank.
                // This runs even when the app is closed, so the user gets notified
                // on their phone immediately via a system notification.
                AlarmReceiver.checkAndFireRankNotification(context)
            }
        }
    }
}
