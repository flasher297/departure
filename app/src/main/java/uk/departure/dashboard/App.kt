package uk.departure.dashboard

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import uk.departure.dashboard.di.appModule

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setUpKoin()
    }


    private fun setUpKoin() {
        startKoin {
            androidContext(this@App)
            modules(listOf(appModule))
        }
    }

}