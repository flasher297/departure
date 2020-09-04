package uk.departure.dashboard.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class EngineInteractor(private val engineRepository: EngineRepository) {

    // To emulate power change we cache previously emmited value.
    // If powers goes up we additionally increase power value, if goes down we decrease.
    // It allows to simulate real life situation when car speed is not changing instantly after adding more power.
    private var previousPowerValue: Float = 0f

    fun startMonitoringEngine(): Flow<EngineOutcomeModel> {
        return engineRepository
            .subscribeToService()
            .buffer(10) // limit amount of data from service if we cant handle all on UI thread
            .map {
                val direction = previousPowerValue.compareTo(it)
                val powerMltplr = if (direction >= 0) {
                    1.4
                } else {
                    0.6
                }
                // Adjust power value and limit max to 1
                EngineOutcomeModel(it, (it * powerMltplr).coerceAtMost(1.0).toFloat())
            }
            .onEach { previousPowerValue = it.speedCoeff }
            .flowOn(Dispatchers.Default)
    }

    fun stopMonitoringEngine() {
        engineRepository.unsubscribeFromService()
    }

}