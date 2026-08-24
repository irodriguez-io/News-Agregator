package io.irodriguez.intentionalreading

import android.app.Application
import io.irodriguez.intentionalreading.di.AppContainer

class IntentionalReadingApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
