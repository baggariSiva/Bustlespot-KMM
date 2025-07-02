package org.softsuave.bustlespot


import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.rememberNavController
import kotlinx.datetime.Month
import org.softsuave.bustlespot.mainnavigation.RootNavigationGraph
import org.softsuave.bustlespot.theme.AppTheme
import org.softsuave.bustlespot.utils.CustomTitleBar


@Composable
internal fun App(onFocusReceived: () -> Unit = {},
                 onDragStart: (Offset) -> Unit = { },
                 onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { change, dragAmount ->},
                 onMinimizeClick:() -> Unit ={},
                 onCloseClick : () -> Unit ={},) {
    val navController = rememberNavController()
    MaterialTheme {
         RootNavigationGraph(navController, onFocusReceived)
    }
}


