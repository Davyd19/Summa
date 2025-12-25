package com.app.summa.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import kotlin.math.absoluteValue

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleHabitReminder(habitId: Long, habitName: String, timeString: String) {
        if (timeString.isBlank()) return

        try {
            val timeParts = timeString.split(":").map { it.toInt() }
            if (timeParts.size < 2) return

            val hour = timeParts[0]
            val minute = timeParts[1]

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // PERBAIKAN: Gunakan ID Unik.
            // ID Habit range: 0 - 999,999. Menggunakan toInt() aman untuk database kecil-menengah.
            val notificationId = (habitId.hashCode().absoluteValue % 1000000)

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", "Waktunya Kebiasaan 🎯")
                putExtra("MESSAGE", "Saatnya melakukan: $habitName")
                putExtra("ID", notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d("SummaNotification", "Habit scheduled: $habitName (ID: $notificationId) at $timeString")

        } catch (e: Exception) {
            Log.e("SummaNotification", "Failed to schedule habit", e)
        }
    }

    fun scheduleTaskReminder(taskId: Long, taskTitle: String, date: String, time: String) {
        if (date.isBlank() || time.isBlank()) return

        try {
            // PERBAIKAN: Gunakan Formatter yang aman
            val localDate = try {
                LocalDate.parse(date)
            } catch (e: DateTimeParseException) {
                // Fallback format jika perlu, atau return
                return
            }

            val localTime = try {
                LocalTime.parse(time)
            } catch (e: DateTimeParseException) {
                return
            }

            val dateTime = LocalDateTime.of(localDate, localTime)
            val triggerTime = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerTime <= System.currentTimeMillis()) return

            // PERBAIKAN: ID Task Offset agar tidak tabrakan dengan Habit
            // Task ID range start from 1,000,000
            val notificationId = (taskId.hashCode().absoluteValue % 1000000) + 1000000

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", "Pengingat Tugas 📝")
                putExtra("MESSAGE", taskTitle)
                putExtra("ID", notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d("SummaNotification", "Task scheduled: $taskTitle (ID: $notificationId) at $date $time")

        } catch (e: Exception) {
            Log.e("SummaNotification", "Failed to schedule task", e)
        }
    }

    fun showImmediateNotification(title: String, message: String) {
        try {
            val id = System.currentTimeMillis().toInt()
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", title)
                putExtra("MESSAGE", message)
                putExtra("ID", id)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelHabitReminder(habitId: Long) {
        try {
            val notificationId = (habitId.hashCode().absoluteValue % 1000000)
            cancelPendingIntent(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        try {
            val notificationId = (taskId.hashCode().absoluteValue % 1000000) + 1000000
            cancelPendingIntent(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelPendingIntent(id: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}