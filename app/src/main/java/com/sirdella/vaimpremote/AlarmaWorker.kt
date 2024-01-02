package com.sirdella.vaimpremote

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*

class AlarmaWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams)
{
    val logger = Logger(appContext)

    override fun doWork(): Result {
        val c = Calendar.getInstance()

        logger.log("Checkeando", "workerlogs")

        val vaimpData = VaimpData(applicationContext, false)
        val vaimpCalls = VaimpCalls()

        for(ip in vaimpData.ips)
        {
            logger.log("Llamando a ${ip.address} (online: ${ip.online})", "workerlogs")
            vaimpCalls.isVAIMP(ip.address){
                logger.log("${ip.address} respondió $it", "workerlogs")
                val prevState = ip.online
                ip.online = it

                if (!prevState && ip.online){
                    logger.log("Lanzando notificacion para ${ip.address}", "workerlogs")
                    val builder = Notification.Builder(applicationContext, "notischecks")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(applicationContext.getString(R.string.check_server_online))
                        .setContentText(ip.address + " " + applicationContext.getString(R.string.check_server_online2))
                        .setSmallIcon(R.drawable.playicon)

                    var notification = builder.build()

                    with(NotificationManagerCompat.from(applicationContext)) {
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.ACCESS_NOTIFICATION_POLICY
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {

                        }
                        // notificationId is a unique int for each notification that you must define.
                        notify(ip.address.hashCode(), builder.build())
                    }
                }

                if (!ip.online)
                {
                    logger.log("Cerrando notificacion para ${ip.address}", "workerlogs")
                    val notificationManager = NotificationManagerCompat.from(applicationContext)
                    notificationManager.cancel(ip.address.hashCode())
                }

                logger.log("worker guardando datos", "workerlogs")
                vaimpData.guardarConfig()
            }
        }

        return Result.success()
    }
}