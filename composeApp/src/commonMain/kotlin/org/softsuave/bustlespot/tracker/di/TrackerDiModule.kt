package org.softsuave.bustlespot.tracker.di

import org.softsuave.bustlespot.tracker.data.TrackerRepository
import org.softsuave.bustlespot.tracker.data.TrackerRepositoryImpl
import org.softsuave.bustlespot.tracker.ui.HomeViewModel
import org.koin.dsl.module

val trackerDiModule = module{
    single<TrackerRepository>{
        TrackerRepositoryImpl(get(),get(),get())
    }
    factory {
        HomeViewModel(get(),get(),get(),get())
    }
}