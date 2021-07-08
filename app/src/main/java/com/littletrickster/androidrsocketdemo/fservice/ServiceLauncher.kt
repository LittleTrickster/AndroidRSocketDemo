package com.littletrickster.androidrsocketdemo.fservice

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class ServiceLauncher(context: Application) {
    companion object {
        const val id = 1.toString()
        const val name = "Foreground service" //visible in notification settings
    }


    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
            channel.setSound(null, null)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)

        }
    }

    fun start(context: Context) {
        val serviceIntent = Intent(context, MyService::class.java)
            .apply { action = MyService.ACTION_START }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun stop(context: Context) {
        val serviceIntent = Intent(context, MyService::class.java)
            .apply { action = MyService.ACTION_STOP }
        context.startService(serviceIntent)
    }

}