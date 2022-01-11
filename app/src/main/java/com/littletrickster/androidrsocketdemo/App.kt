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
        super.onCreate()
    }
}