package uk.departure.dashboard

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var speedometerView: SpeedometerView

    private val engineOutPut = MutableStateFlow(0.0f)
    val engineOutPut2: StateFlow<Float>
        get() = engineOutPut

    @SuppressLint("InlinedApi")
    private val hideRunnable = Runnable {
        speedometerView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        speedometerView = findViewById(R.id.speedometer_view)

        makeFullScreenNonSleep()
        startService()

        subscribe()
    }

    private fun subscribe() {
        GlobalScope.launch(Dispatchers.Main) {
            engineOutPut2.buffer(10).collect {
                speedometerView.updateRelatively(it)
            }
        }
    }

    private var mService: IEngineInterface? = null

    private val mCallback = object : IEngineCallback.Stub() {

        override fun valueChanged(value: Float) {
            engineOutPut.value =  value
        }

    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = IEngineInterface.Stub.asInterface(service)

            try {
                mService?.registerCallback(mCallback)
            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

        }

        override fun onServiceDisconnected(className: ComponentName) {
            mService = null
        }
    }


    // TODO: unsubscribe on stop
    fun stopEService() {
        try {
            mService?.unregisterCallback(mCallback)
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
        }

        unbindService(mConnection)
    }

    private fun startService() {
        val intent = Intent(this, EngineService::class.java)
        intent.action = EngineService::class.java.name
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private fun makeFullScreenNonSleep() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        speedometerView.post(hideRunnable)
    }

}