package com.sirdella.vaimpremote

import android.app.Notification
import android.media.MediaSession2
import android.media.MediaSession2Service
import android.os.Build
import androidx.annotation.RequiresApi

private var mediaSession: MediaSession2? = null


@RequiresApi(Build.VERSION_CODES.Q)
class MediaSession : MediaSession2Service() {
    override fun onCreate() {
        super.onCreate()

    }

    override fun onGetSession(controllerInfo: MediaSession2.ControllerInfo): MediaSession2? = mediaSession

    override fun onUpdateNotification(session: MediaSession2): MediaNotification {
        val notification = Notification() //me lo invente
        return MediaNotification(0, notification)
    }
}