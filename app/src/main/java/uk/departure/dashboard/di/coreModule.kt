package uk.departure.dashboard.di

import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import uk.departure.dashboard.data.EngineRepositoryImpl
import uk.departure.dashboard.domain.EngineInteractor
import uk.departure.dashboard.domain.EngineRepository
import uk.departure.dashboard.presentation.DashboardViewModel

val appModule = module {

    factory<EngineRepository> { EngineRepositoryImpl(androidContext()) }

    viewModel { DashboardViewModel(get()) }

    factory { EngineInteractor(get()) }

}

