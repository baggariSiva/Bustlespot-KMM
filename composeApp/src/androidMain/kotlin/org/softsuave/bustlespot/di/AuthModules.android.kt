package org.softsuave.bustlespot.auth.di

import android.media.projection.MediaProjection
import com.company.app.screenshot.ScreenshotProvider
import org.softsuave.bustlespot.MediaProjectionHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module


actual val platformModule: Module = org.koin.dsl.module {
    single<MediaProjection> {
        MediaProjectionHolder.mediaProjection
            ?: error("MediaProjection not available. Request permission first.")
    }
    factory {
        ScreenshotProvider(androidContext(), get())
    }
}