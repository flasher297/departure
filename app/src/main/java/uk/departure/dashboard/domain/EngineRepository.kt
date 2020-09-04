package uk.departure.dashboard.domain

import kotlinx.coroutines.flow.Flow

interface EngineRepository {

    fun unsubscribeFromService()

    fun subscribeToService(): Flow<Float>

}