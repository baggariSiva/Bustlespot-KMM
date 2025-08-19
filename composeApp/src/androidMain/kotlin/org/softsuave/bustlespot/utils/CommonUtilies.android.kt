package org.softsuave.bustlespot.utils

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable



@Composable
actual fun HandleBackPress(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
