package com.littletrickster.androidrsocketdemo.fservice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.littletrickster.androidrsocketdemo.R
import com.littletrickster.androidrsocketdemo.mainactivity.MainActivity
import com.littletrickster.androidrsocketdemo.server.MyKtorServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.ext.android.inject

class MyService : Service() {
    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        val running = MutableStateFlow<Boolean>(false)
        val lock = Mutex()
    }

    val server: MyKtorServer by inject()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
//                else -> crash
        }

        return START_REDELIVER_INTENT
    }

    private fun stop() {
        running.tryEmit(false)
        stopServerWithLock()
        stopForeground(true)
        stopSelf()
    }


    private fun runServerWithLock() {
        GlobalScope.launch {
            lock.withLock {
                server.runServer()
            }
        }
    }


    private fun stopServerWithLock() {
        GlobalScope.launch {
            lock.withLock {
                server.stopServer()
            }
        }
    }

    private fun start() {
        if (running.value) return
        runServerWithLock()
        running.tryEmit(true)
        val notification = buildNotification()
        // Start foreground service.
        startForeground(1, notification)
    }


    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
            .addNextIntent(intent)

        val resultPendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)


        val builder = NotificationCompat.Builder(this, ServiceLauncher.id)
            .setDefaults(0)
            .setContentTitle("Web server launched")
            .setSound(null)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_android_black_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(resultPendingIntent)
        return builder.build()
    }

    override fun onBind(intent: Intent): IBinder {
        throw NotImplementedError("An operation is not implemented")
    }
}