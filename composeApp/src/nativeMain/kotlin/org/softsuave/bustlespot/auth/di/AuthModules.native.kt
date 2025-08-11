package org.softsuave.bustlespot.auth.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.softsuave.bustlespot.background.PostingServiceManager

actual val platformModule: Module = module {
    single { PostingServiceManager() }
}