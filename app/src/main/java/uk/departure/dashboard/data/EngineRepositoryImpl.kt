package uk.departure.dashboard.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.WorkerThread
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.departure.dashboard.IEngineCallback
import uk.departure.dashboard.IEngineInterface
import uk.departure.dashboard.domain.EngineRepository

@ExperimentalCoroutinesApi
class EngineRepositoryImpl(private val applicationContext: Context) : EngineRepository {

    // Preserves last received value and makes distinctUntilChanged
    private val engineOutput = MutableStateFlow(0.0f)
    private val engineOutputValue: StateFlow<Float>
        get() = engineOutput

    private var engineService: IEngineInterface? = null
    private val engineCallback = object : IEngineCallback.Stub() {

        @WorkerThread
        override fun outputPower(value: Float) {
            engineOutput.value = value
        }
    }

    private val engineServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            engineService = IEngineInterface.Stub.asInterface(service)

            try {
                engineService?.registerOutcomeCallback(engineCallback)
            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even
                // do anything with it;
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            engineService = null
        }
    }

    override fun unsubscribeFromService() {
        try {
            engineService?.unregisterOutcomeCallback(engineCallback)
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
        }
        applicationContext.unbindService(engineServiceConnection)
    }

    override fun subscribeToService(): Flow<Float> {
        val intent = Intent(applicationContext, EngineService::class.java)
        intent.action = EngineService::class.java.name
        applicationContext.bindService(intent, engineServiceConnection, Context.BIND_AUTO_CREATE)
        return engineOutputValue
    }
}