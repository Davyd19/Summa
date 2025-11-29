package com.app.summa.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.app.summa.MainActivity
// Pastikan resource R terimport. Jika merah, biasanya karena belum build atau package beda.
// import com.app.summa.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Ambil data dari intent
        val title = intent.getStringExtra("TITLE") ?: "Pengingat Summa"
        val message = intent.getStringExtra("MESSAGE") ?: "Waktunya bertindak!"
        val id = intent.getIntExtra("ID", 0)

        showNotification(context, title, message, id)
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "summa_proactive_channel"

        // 1. Buat Channel (Wajib untuk Android 8.0 Oreo ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Summa Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat untuk Kebiasaan dan Tugas"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Intent saat notifikasi diklik (Membuka MainActivity)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Bangun Notifikasi
        val notification = NotificationCompat.Builder(context, channelId)
            // Gunakan icon default android jika icon app belum ada, atau R.mipmap.ic_launcher
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}