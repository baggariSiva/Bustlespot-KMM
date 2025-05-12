package org.softsuave.bustlespot.locationmodule

import com.mmk.kmpnotifier.notification.NotificationImage
import com.mmk.kmpnotifier.notification.Notifier
import com.mmk.kmpnotifier.notification.NotifierManager


fun sendLocalNotification(notificationId: Int, locationInfo: String) {


    val notifier = NotifierManager.getLocalNotifier()
    notifier.notify {
        id = notificationId
        title = "Current Location"
        body = locationInfo
        payloadData = mapOf(
            Notifier.KEY_URL to "https://github.com/mirzemehdi/KMPNotifier/",
            "extraKey" to "randomValue"
        )
        image =
            NotificationImage.Url("https://github.com/user-attachments/assets/a0f38159-b31d-4a47-97a7-cc230e15d30b")
    }
}