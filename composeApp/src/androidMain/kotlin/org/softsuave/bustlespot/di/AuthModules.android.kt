package org.softsuave.bustlespot.auth.di

import android.app.Activity
import androidx.activity.ComponentActivity
import org.koin.core.module.Module


actual val platformModule: Module = org.koin.dsl.module {
    factory { (activity: Activity) -> activity }
    factory { (activity: ComponentActivity) -> activity }
}