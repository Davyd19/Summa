package com.app.summa.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.app.summa.data.model.Habit
import com.app.summa.data.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // --- TASK NOTIFICATIONS ---

    fun scheduleTaskNotification(task: Task) {
        if (task.scheduledTime == null || task.isCompleted) return

        try {
            val date = LocalDate.parse(task.scheduledDate)
            val time = LocalTime.parse(task.scheduledTime)
            val scheduleTime = LocalDateTime.of(date, time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (scheduleTime <= System.currentTimeMillis()) return

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TYPE", "TASK")
                putExtra("ID", task.id)
                putExtra("TITLE", task.title)
                putExtra("MESSAGE", "Waktunya mengerjakan: ${task.title}")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ("TASK_" + task.id).hashCode(), // Unique Request Code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactAlarm(scheduleTime, pendingIntent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelTaskNotification(task: Task) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("TASK_" + task.id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // --- HABIT REMINDERS (Fungsi yang sebelumnya hilang) ---

    fun scheduleHabitReminder(habit: Habit) {
        if (habit.reminderTime.isBlank()) return

        try {
            val times = habit.reminderTime.split(",")
            times.forEachIndexed { index, timeStr ->
                if (timeStr.isBlank()) return@forEachIndexed

                val time = LocalTime.parse(timeStr)
                val now = LocalDateTime.now()
                var scheduleTime = LocalDateTime.of(now.toLocalDate(), time)

                // Jika waktu sudah lewat hari ini, jadwalkan besok
                if (scheduleTime.isBefore(now)) {
                    scheduleTime = scheduleTime.plusDays(1)
                }

                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("TYPE", "HABIT")
                    putExtra("ID", habit.id)
                    putExtra("TITLE", "Pengingat Kebiasaan")
                    putExtra("MESSAGE", "Waktunya untuk: ${habit.name} ${habit.icon}")
                }

                val uniqueId = ("HABIT_" + habit.id + "_" + index).hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    uniqueId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Jadwalkan berulang setiap hari
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    // Fallback jika tidak ada izin exact alarm
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        scheduleTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                } else {
                    // Note: setRepeating tidak exact di Android modern (Doze mode).
                    // Untuk presisi tinggi, harus reschedule manual setiap kali alarm berbunyi.
                    // Di sini kita gunakan setRepeating standar agar lebih hemat baterai.
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        scheduleTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelHabitReminder(habit: Habit) {
        // Cancel semua kemungkinan slot reminder (misal max 5 slot)
        for (i in 0 until 5) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val uniqueId = ("HABIT_" + habit.id + "_" + i).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    // --- DAILY BRIEFING ---

    fun scheduleDailyBriefing() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7) // Jam 7 Pagi
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TYPE", "BRIEFING")
            putExtra("TITLE", "Morning Briefing")
            putExtra("MESSAGE", "Lihat agenda dan progres Anda hari ini!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001, // ID Khusus Briefing
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // --- HELPER: SAFE EXACT ALARM ---
    private fun scheduleExactAlarm(timeInMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                Log.w("NotificationScheduler", "Izin Exact Alarm tidak diberikan. Menggunakan setWindow.")
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    10 * 60 * 1000, // Toleransi 10 menit
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
}