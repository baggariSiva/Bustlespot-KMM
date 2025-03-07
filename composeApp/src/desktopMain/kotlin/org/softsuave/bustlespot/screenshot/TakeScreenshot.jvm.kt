package org.softsuave.bustlespot.screenshot

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.File
import javax.imageio.ImageIO

fun captureFullScreenScreenshot(imageName: String): ImageBitmap? {
    return try {
        // Get the entire screen dimensions
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenRect = Rectangle(screenSize)

        // Create Robot instance
        val robot = Robot()

        // Capture the entire screen
        val screenshot = robot.createScreenCapture(screenRect)

        // Save the image to the specified file in the directory
        val file = File("$imageName.png")
        ImageIO.write(screenshot, "png", file)

        println("Screenshot saved at: ${file.absolutePath}")

        // Convert BufferedImage to ImageBitmap and return
        fileToImageBitmap(
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun fileToImageBitmap(file: File): ImageBitmap {
    val image = Image.makeFromEncoded(file.readBytes())
    return image.toComposeImageBitmap()
}

actual fun takeScreenShot(): ImageBitmap? {
    return captureFullScreenScreenshot(
        "screen"
    )
}