package org.softsuave.bustlespot

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import bustlespot.composeapp.generated.resources.Res
import bustlespot.composeapp.generated.resources.logoRed
import com.github.kwhat.jnativehook.GlobalScreen
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.jetbrains.compose.resources.painterResource
import org.softsuave.bustlespot.di.initKoin
import java.awt.Button
import java.awt.Desktop
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.Label
import java.net.ServerSocket
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Prevent multiple instances using a lock on a specific port
    val singleInstancePort = 65432
    val lockSocket: ServerSocket = try {
        ServerSocket(singleInstancePort).also {
            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    it.close()
                } catch (_: Exception) {
                }
            })
        }
    } catch (e: Exception) {
        JOptionPane.showMessageDialog(
            null,
            "Bustlespot is already running.",
            "Instance Already Running",
            JOptionPane.WARNING_MESSAGE
        )
        return
    }
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        print("asdasd")
        e.printStackTrace()
        print("erorr ${e} is here")
        Dialog(Frame(), e.message ?: "Error").apply {
            layout = FlowLayout()
            val label = Label(e.message)
            add(label)
            val button = Button("OK").apply {
                addActionListener { dispose() }
            }
            add(button)
            setSize(300,300)
            isVisible = true
        }
    }

    application {
        // Cleanup on close
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                GlobalScreen.unregisterNativeHook()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        initKoin()

        Window(
            title = "Bustlespot",
            decoration = WindowDecoration.SystemDefault,
            state = rememberWindowState(size = DpSize(width = 460.dp, height = 785.dp)),
            onCloseRequest = ::exitApplication,
            resizable = false,
            icon = painterResource(Res.drawable.logoRed),
        ) {
            App(
                onFocusReceived = {
                    val desktop = Desktop.getDesktop()
                    if (desktop.isSupported(Desktop.Action.APP_EVENT_FOREGROUND)) {
                        // works for Mac OS
                        desktop.requestForeground(true)
                        window.toFront()
                        window.requestFocus()
                    } else {
                        //works for Windows
                        if (window.state == Frame.ICONIFIED) {
                            window.state = Frame.NORMAL
                        }
                        SwingUtilities.invokeLater {
                            window.isAlwaysOnTop = true
                            window.toFront()
                            window.requestFocus()
                            window.isAlwaysOnTop = false
                        }
                    }
                },
                onMinimizeClick = {
                    window.let {
                        it.isMinimized = false
                        SwingUtilities.invokeLater { it.isMinimized = true }
                    }
                },
                onCloseClick = {
                    exitApplication()
                }
            )
        }
    }
}