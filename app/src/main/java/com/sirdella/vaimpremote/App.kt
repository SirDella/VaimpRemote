package com.sirdella.vaimpremote

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class App: Application() {
    var repoVaimp: VaimpData? = null
    val logger = Logger(this)

    override fun onCreate() {
        super.onCreate()
        logger.DeleteLogs()
        logger.log("Inicio App", "Flowlog")

        GlobalScope.launch(Dispatchers.IO) {
            repoVaimp = VaimpData(applicationContext)
        }

        //notificacion:
        val serviceChannel = NotificationChannel(
            "canaldenatis",
            "Vaimp Controls",
            NotificationManager.IMPORTANCE_LOW
        )
        val checksChannel = NotificationChannel(
            "notischecks",
            "Background checks",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(checksChannel)

        logger.log("Fin App", "Flowlog")
    }
}
