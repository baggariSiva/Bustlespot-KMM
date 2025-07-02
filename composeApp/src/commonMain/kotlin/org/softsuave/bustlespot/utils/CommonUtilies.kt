package org.softsuave.bustlespot.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

expect fun isAndroid(): Boolean


@Composable
expect fun handleBackPress(
    onBack: () -> Unit = {}
)


val BustleSpotRed : Color
    get() = Color(242, 60, 75, 255)


fun <T> List<T>.moveToFirst(item: T): List<T> {
    val mutableList = this.toMutableList()
    if (mutableList.remove(item)) { // Only move if the item exists
        mutableList.add(0, item)
    }
    return mutableList
}
//
//fun getCurrentDateTime(): String {
//    val zoneId = ZoneId.of("Asia/Kolkata") // +05:30
//    val currentDateTime = ZonedDateTime.now(zoneId)
//
//    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSXXX")
//    return currentDateTime.format(formatter)
//}