//package org.softsuave.bustlespot.background
//
//import platform.UserNotifications.*
//import platform.Foundation.*
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlin.coroutines.resume
//
//actual class NotificationManager {
//    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
//
//    actual suspend fun requestPermission(): Boolean {
//        return suspendCancellableCoroutine { continuation ->
//            notificationCenter.requestAuthorizationWithOptions(
//                options = UNAuthorizationOptionAlert or
//                        UNAuthorizationOptionSound or
//                        UNAuthorizationOptionBadge
//            ) { isGranted, error ->
//                continuation.resume(granted)
//            }
//        }
//    }
//
//    actual suspend fun scheduleNotification(
//        id: String,
//        title: String,
//        body: String,
//        delayInSeconds: Long
//    ) {
//        // Create notification content
//        val content = UNMutableNotificationContent().apply {
//            setTitle(title)
//            setBody(body)
//            setSound(UNNotificationSound.defaultSound())
//        }
//
//        // Create trigger (immediate or delayed)
//        val trigger = if (delayInSeconds > 0) {
//            UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
//                timeInterval = delayInSeconds.toDouble(),
//                repeats = false
//            )
//        } else null
//
//        // Create request
//        val request = UNNotificationRequest.requestWithIdentifier(
//            identifier = id,
//            content = content,
//            trigger = trigger
//        )
//
//        // Schedule notification
//        suspendCancellableCoroutine<Unit> { continuation ->
//            notificationCenter.addNotificationRequest(request) { error ->
//                continuation.resume(Unit)
//            }
//        }
//    }
//
//    actual suspend fun cancelNotification(id: String) {
//        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(id))
//        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(id))
//    }
//
//    actual suspend fun cancelAllNotifications() {
//        notificationCenter.removeAllPendingNotificationRequests()
//        notificationCenter.removeAllDeliveredNotifications()
//    }
//}