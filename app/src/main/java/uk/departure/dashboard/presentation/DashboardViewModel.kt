package uk.departure.dashboard.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import uk.departure.dashboard.domain.EngineInteractor
import uk.departure.dashboard.domain.EngineOutcomeModel

open class DashboardViewModel(private val engineInteractor: EngineInteractor) : ViewModel() {

    // Chain will be executed on active livedata subscription
    val engineOutcome: LiveData<EngineOutcomeModel> =
        engineInteractor.startMonitoringEngine().asLiveData()

    override fun onCleared() {
        engineInteractor.stopMonitoringEngine()
        super.onCleared()
    }

}