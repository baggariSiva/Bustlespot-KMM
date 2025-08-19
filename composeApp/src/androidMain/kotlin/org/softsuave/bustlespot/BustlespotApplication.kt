package org.softsuave.bustlespot

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.softsuave.bustlespot.di.initKoin
import org.softsuave.bustlespot.network.NetworkMonitorProvider

class BustlespotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkMonitorProvider.init(context = this)
        initKoin {
            androidLogger()
            androidContext(this@BustlespotApplication)
        }

    }
}