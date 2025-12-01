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
import java.util.Calendar
import kotlin.math.absoluteValue

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Menjadwalkan pengingat berulang untuk Kebiasaan (Habit)
     * Format timeString: "HH:mm" (misal "08:00")
     */
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

            // Jika waktu sudah lewat hari ini, jadwalkan untuk besok
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", "Waktunya Kebiasaan 🎯")
                putExtra("MESSAGE", "Saatnya melakukan: $habitName")
                // PERBAIKAN: Gunakan ID yang aman (Range 0 - 999,999 untuk Habit)
                val safeId = (habitId % 1000000).toInt().absoluteValue
                putExtra("ID", safeId)
            }

            // Gunakan ID unik untuk PendingIntent
            val pendingIntentId = (habitId % 1000000).toInt().absoluteValue

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                pendingIntentId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Gunakan setRepeating untuk kebiasaan harian
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d("SummaNotification", "Habit scheduled: $habitName (ID: $habitId -> $pendingIntentId) at $timeString")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menjadwalkan pengingat sekali jalan untuk Tugas (Task)
     * Format date: "yyyy-MM-dd", time: "HH:mm"
     */
    fun scheduleTaskReminder(taskId: Long, taskTitle: String, date: String, time: String) {
        if (date.isBlank() || time.isBlank()) return

        try {
            val localDate = LocalDate.parse(date)
            val localTime = LocalTime.parse(time)
            val dateTime = LocalDateTime.of(localDate, localTime)

            val triggerTime = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerTime <= System.currentTimeMillis()) return // Jangan jadwalkan masa lalu

            // PERBAIKAN: Gunakan ID yang aman (Range 1,000,000+ untuk Task)
            // Menggunakan hashCode() dari kombinasi ID agar unik tapi tetap konsisten
            val safeId = (taskId.hashCode().absoluteValue % 1000000) + 1000000

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", "Pengingat Tugas 📝")
                putExtra("MESSAGE", taskTitle)
                putExtra("ID", safeId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                safeId, // RequestCode unik
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Gunakan setExactAndAllowWhileIdle untuk tugas penting agar akurat
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d("SummaNotification", "Task scheduled: $taskTitle (ID: $taskId -> $safeId) at $date $time")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // FITUR BARU: Notifikasi Sistem (untuk Hukuman/Level Up)
    fun showImmediateNotification(title: String, message: String) {
        try {
            val id = System.currentTimeMillis().toInt() // ID unik sementara
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
            val pendingIntentId = (habitId % 1000000).toInt().absoluteValue
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                pendingIntentId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        try {
            val safeId = (taskId.hashCode().absoluteValue % 1000000) + 1000000
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                safeId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}