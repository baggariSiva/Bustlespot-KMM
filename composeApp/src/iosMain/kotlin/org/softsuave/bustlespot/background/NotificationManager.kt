package org.softsuave.bustlespot.background

import platform.UserNotifications.*
import platform.Foundation.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class NotificationManager {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    actual suspend fun requestPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or
                        UNAuthorizationOptionSound or
                        UNAuthorizationOptionBadge
            ) { isGranted, error ->
                println("iOS Notification permission granted: $isGranted")
                if (error != null) {
                    println("iOS Notification permission error: ${error.localizedDescription}")
                }
                continuation.resume(isGranted)
            }
        }
    }

    actual suspend fun scheduleNotification(
        id: String,
        title: String,
        body: String,
        delayInSeconds: Long
    ) {
        println("Scheduling notification: id=$id, title=$title, delay=$delayInSeconds")

        // Create notification content
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
            // Add badge number
            setBadge(NSNumber.numberWithInt(1))
        }

        // Create trigger - FIXED: Handle immediate notifications properly
        val trigger = when {
            delayInSeconds > 0 -> {
                // For delayed notifications
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = delayInSeconds.toDouble(),
                    repeats = false
                )
            }
            else -> {
                // For immediate notifications, use a very small delay
                // iOS doesn't allow null trigger for immediate notifications in some cases
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = 0.1, // 100ms delay
                    repeats = false
                )
            }
        }

        // Create request
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id,
            content = content,
            trigger = trigger
        )

        // Schedule notification with error handling
        suspendCancellableCoroutine<Unit> { continuation ->
            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error scheduling notification: ${error.localizedDescription}")
                } else {
                    println("Notification scheduled successfully: $id")
                }
                continuation.resume(Unit)
            }
        }
    }

    actual suspend fun cancelNotification(id: String) {
        println("Cancelling notification: $id")
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }

    actual suspend fun cancelAllNotifications() {
        println("Cancelling all notifications")
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
    }

//    // Additional helper functions for debugging
//    suspend fun checkPendingNotifications() {
//        suspendCancellableCoroutine<Unit> { continuation ->
//            notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
//                println("Pending notifications count: ${requests?.size ?: 0}")
//                requests?.forEach { request ->
//                    println("Pending: ${request.identifier} - ${request.content.title}")
//                }
//                continuation.resume(Unit)
//            }
//        }
//    }
//
//    suspend fun checkNotificationSettings() {
//        suspendCancellableCoroutine<Unit> { continuation ->
//            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
//                println("Notification settings:")
//                println("- Authorization Status: ${settings?.authorizationStatus}")
//                println("- Alert Setting: ${settings?.alertSetting}")
//                println("- Sound Setting: ${settings?.soundSetting}")
//                println("- Badge Setting: ${settings?.badgeSetting}")
//                continuation.resume(Unit)
//            }
//        }
//    }
}