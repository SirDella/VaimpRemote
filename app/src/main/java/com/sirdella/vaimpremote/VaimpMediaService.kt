package com.sirdella.vaimpremote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Locale.Category

class VaimpMediaService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var builder = NotificationCompat.Builder(this, "coso")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("My notification")
            .setContentText("Much longer text that cannot fit one line...")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Much longer text that cannot fit one line..."))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)

        val notification = builder.build()


        val notificationChannel = NotificationChannel("1", "canal", NotificationManager.IMPORTANCE_NONE)

        startForeground(notificationChannel.id.toInt(), notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null //todo
    }
}