package org.softsuave.bustlespot.background


expect class NotificationManager() {
    suspend fun requestPermission(): Boolean
    suspend fun scheduleNotification(
        id: String,
        title: String,
        body: String,
        delayInSeconds: Long = 0
    )
    suspend fun cancelNotification(id: String)
    suspend fun cancelAllNotifications()
}
