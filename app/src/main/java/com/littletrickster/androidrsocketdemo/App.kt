package com.littletrickster.androidrsocketdemo

import android.app.Application
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

class App : Application() {
    override fun onCreate() {
        startKoin {
            androidContext(this@App)
            modules(KoinModules.simpleModels, KoinModules.viewModels)
        }

        //my workaround for ClosedReceiveChannelException crash when rSocket client loses connection
        //probably unsafe
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable ->
            when {
                paramThrowable is ClosedReceiveChannelException && paramThrowable.stackTrace.find { it.fileName == "DefaultWebSocketSessionImpl.kt" } != null -> {
                }
                oldHandler != null -> oldHandler.uncaughtException(
                    paramThread,
                    paramThrowable
                )
                else -> exitProcess(2)
            }
        }

        super.onCreate()
    }
}