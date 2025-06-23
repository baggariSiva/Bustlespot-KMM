package org.softsuave.bustlespot.di

import org.softsuave.bustlespot.auth.di.platformModule
import org.softsuave.bustlespot.auth.di.sharedModules
import org.softsuave.bustlespot.organisation.di.organisationModule
import org.softsuave.bustlespot.tracker.di.trackerDiModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.softsuave.bustlespot.organisationmodule.di.organisationModulesModule

fun initKoin(
    config: KoinAppDeclaration? = null
) {
    startKoin {
        config?.invoke(this)
        modules(koinGlobalModule,sharedModules, platformModule,organisationModule,organisationModulesModule,trackerDiModule)
    }
}