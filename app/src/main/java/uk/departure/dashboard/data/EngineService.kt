package uk.departure.dashboard.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import kotlinx.coroutines.*
import uk.departure.dashboard.IEngineCallback
import uk.departure.dashboard.IEngineInterface
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class EngineService : Service() {

    private lateinit var serviceJob: Job
    private lateinit var serviceScope: CoroutineScope

    private var engineJob: Job? = null

    private val mCallbacks: RemoteCallbackList<IEngineCallback> = RemoteCallbackList()

    private val binder = object : IEngineInterface.Stub() {
        override fun registerOutcomeCallback(cb: IEngineCallback?) {
            cb?.let {
                mCallbacks.register(it)
            }
            startEngine()
        }

        override fun unregisterOutcomeCallback(cb: IEngineCallback?) {
            cb?.let {
                mCallbacks.unregister(it)
            }
            engineJob?.cancel()
        }
    }

    /**
     * Engine produces output from 0 to 1.
     * 0 - is stopped
     * 1- is 100% max power
     */
    private fun startEngine() {
        if (engineJob?.isActive == true) {
            return
        }
        engineJob = serviceScope.launch {
            var startTime = 0L
            while (isActive) {
                delay(100)  // delay() stops eternal loop on coroutine cancellation
                val radian = Math.toRadians(startTime.toDouble())
                val coef = abs((sin(8 * radian) + sin(0.4 * sqrt(radian))) / 2)
                val receiversCount = mCallbacks.beginBroadcast()
                for (i in 0 until receiversCount) {
                    try {
                        mCallbacks.getBroadcastItem(i).outputPower(coef.toFloat().coerceIn(0f, 1f))
                    } catch (e: RemoteException) {
                        // The RemoteCallbackList will take care of removing
                        // the dead object for us.
                    }
                }
                mCallbacks.finishBroadcast()
                startTime++
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        serviceJob = SupervisorJob()
        serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    }

    override fun onBind(intent: Intent): IBinder {
        // Return the interface
        return binder
    }


    override fun onUnbind(intent: Intent?): Boolean {
        engineJob?.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCallbacks.kill()
        serviceJob.cancel()
    }
}
