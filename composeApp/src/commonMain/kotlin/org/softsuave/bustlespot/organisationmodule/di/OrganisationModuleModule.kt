package org.softsuave.bustlespot.organisationmodule.di


import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.softsuave.bustlespot.organisationmodule.data.OrganisationModuleRepository
import org.softsuave.bustlespot.organisationmodule.data.OrganisationModuleRepositoryImpl
import org.softsuave.bustlespot.organisationmodule.ui.OrganisationModuleViewModel

val organisationModulesModule = module {
    single<OrganisationModuleRepository> {
        OrganisationModuleRepositoryImpl(get(),get())
    }

    viewModelOf(::OrganisationModuleViewModel)
}
