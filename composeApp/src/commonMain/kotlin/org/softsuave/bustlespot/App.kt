package org.softsuave.bustlespot



import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.navigation.compose.rememberNavController
import org.softsuave.bustlespot.mainnavigation.RootNavigationGraph


@Composable
internal fun App(onFocusReceived: () -> Unit = {},
                 onDragStart: (Offset) -> Unit = { },
                 onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { change, dragAmount ->},
                 onMinimizeClick:() -> Unit ={},
                 onCloseClick : () -> Unit ={},) {
    val navController = rememberNavController()
    val currentPlatform = getPlatform()
    MaterialTheme {
         RootNavigationGraph(navController, onFocusReceived)
    }
}


