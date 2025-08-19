package org.softsuave.bustlespot.auth.di


import org.softsuave.bustlespot.auth.SignOutUseCase
import org.softsuave.bustlespot.auth.signin.data.SignInRepository
import org.softsuave.bustlespot.auth.signin.data.SignInRepositoryImpl
import org.softsuave.bustlespot.auth.signin.presentation.LoginViewModel
import org.softsuave.bustlespot.auth.signup.data.SignUpRepository
import org.softsuave.bustlespot.auth.signup.data.SignUpRepositoryImpl
import org.softsuave.bustlespot.di.provideHttpClient
import org.softsuave.bustlespot.tracker.ui.HomeViewModelForTimer
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


expect val platformModule: Module


val sharedModules = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModelForTimer)


    single {
        provideHttpClient(
            get(), get()
        )
    }
    single { org.softsuave.bustlespot.SessionManager(get(),get()) }
    single<SignInRepository> { SignInRepositoryImpl(get()) }
    single<SignUpRepository> { SignUpRepositoryImpl(get()) }

    single<SignOutUseCase> {
        SignOutUseCase(get(), get())
    }
}

