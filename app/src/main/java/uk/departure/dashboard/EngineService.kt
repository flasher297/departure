package uk.departure.dashboard

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class EngineService : Service() {

    private lateinit var serviceJob: Job
    private lateinit var serviceScope: CoroutineScope

    private var engineJob: Job? = null

    private val mCallbacks: RemoteCallbackList<IEngineCallback> =
        RemoteCallbackList<IEngineCallback>()

    private val binder = object : IEngineInterface.Stub() {
        override fun registerCallback(cb: IEngineCallback?) {
            cb?.let {
                mCallbacks.register(it)
            }
            startEngine()
        }

        override fun unregisterCallback(cb: IEngineCallback?) {
            cb?.let {
                mCallbacks.unregister(it)
            }
            serviceJob.cancel()
        }
    }

    private fun startEngine() {
        serviceScope.launch {
            var startTime = 0L

            while (true) {
                delay(100)  // stop eternal loop on coroutine cancellation
                val radian = Math.toRadians(startTime.toDouble())
                val coef = abs(
                    (
                            sin(8 * radian) + sin(0.4 * sqrt(radian))) / 2
                )
                val receiversCount = mCallbacks.beginBroadcast()
                for (i in 0 until receiversCount) {
                    try {
                        mCallbacks.getBroadcastItem(i).valueChanged(coef.toFloat().coerceIn(0f, 1f))
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
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCallbacks.kill()
        serviceJob.cancel()
    }
}
