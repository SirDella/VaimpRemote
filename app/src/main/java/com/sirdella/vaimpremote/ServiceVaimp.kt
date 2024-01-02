package com.sirdella.vaimpremote

import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.session.MediaSession
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Style
import java.util.*

class ServiceVaimp : Service() {
    lateinit var notification: Notification

    private fun iniciarService(){
        val bm = BitmapFactory.decodeResource(resources,  R.drawable.defaultsongimage)
        val mediaSession = MediaSession(this, "canal")

        val mediaStyle = Notification.MediaStyle().setMediaSession(mediaSession.sessionToken)

        var builder = Notification.Builder(this, "canaldenatis")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ola")
            .setContentText("kease")
            .setLargeIcon(bm)
            .setStyle(mediaStyle)

        notification = builder.build()

        startForeground(5045, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if(intent?.action == Acciones.INICIAR.toString()) iniciarService()
        if(intent?.action == Acciones.DETENER.toString()) {stopSelf()}

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null //todo
    }

    enum class Acciones {
        INICIAR,
        DETENER
    }
}