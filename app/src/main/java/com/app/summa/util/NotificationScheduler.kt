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
                // Kirim ID sebagai Int ke Receiver agar konsisten
                putExtra("ID", habitId.toInt())
            }

            // Gunakan ID unik untuk PendingIntent (Long -> Int)
            // hashCode() bisa jadi alternatif jika ID sangat besar, tapi toInt() aman untuk auto-inc < 2 milyar
            val pendingIntentId = habitId.toInt()

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

            Log.d("SummaNotification", "Habit scheduled: $habitName (ID: $habitId) at $timeString")

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

            // Offset ID agar tidak bentrok dengan Habit. Habit ID range 0-MaxInt, Task kita geser.
            // Strategi lebih aman: Gunakan ID negatif untuk Task atau range berbeda.
            // Di sini kita gunakan ID asli taskId.toInt() tapi kita tambahkan flag/beda request code logika
            // Agar aman dan sederhana: Task ID kita XOR dengan mask atau semacamnya,
            // tapi cara paling mudah: Task ID pakai request code negatif (jika memungkinkan) atau offset besar.
            // Mari gunakan offset 1000000 seperti sebelumnya tapi pastikan taskId tidak null.

            val notificationId = taskId.toInt()
            // Kita pakai request code yang berbeda range.
            // Asumsi Habit ID < 1.000.000. Task ID kita mulai dr range berbeda di PendingIntent?
            // Tidak perlu, asalkan unik per alarm.
            // Kita gunakan taskId.toInt() + 1000000 untuk requestCode
            val requestCode = taskId.toInt() + 1000000

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", "Pengingat Tugas 📝")
                putExtra("MESSAGE", taskTitle)
                putExtra("ID", notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Gunakan setExactAndAllowWhileIdle untuk tugas penting agar akurat
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d("SummaNotification", "Task scheduled: $taskTitle at $date $time")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelHabitReminder(habitId: Long) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.toInt(),
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
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt() + 1000000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}