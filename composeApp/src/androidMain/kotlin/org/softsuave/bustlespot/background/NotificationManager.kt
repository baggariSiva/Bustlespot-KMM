package org.softsuave.bustlespot.background

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import org.softsuave.bustlespot.R
import android.app.NotificationManager as AndroidNotificationManager

class NotificationManager(
    private val context: Context,
    private val notificationChannelId: String,
    private val notificationChannelName: String,
    private val notificationChannelDescription: String
) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val androidNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

    companion object {
        private const val DEFAULT_NOTIFICATION_ID = 1001
    }

    /**
     * Creates a notification channel for Android O (API 26) and above
     */
    fun createNotificationChannel() {
        val importance = AndroidNotificationManager.IMPORTANCE_LOW // Changed to LOW for service notifications
        val channel = NotificationChannel(
            notificationChannelId,
            notificationChannelName,
            importance
        ).apply {
            description = notificationChannelDescription
            enableLights(false) // Disabled for service notifications
            enableVibration(false) // Disabled for service notifications
            setSound(null, null) // No sound for service notifications
            setShowBadge(true) // No badge for service notifications
        }

        androidNotificationManager.createNotificationChannel(channel)
    }

    /**
     * Shows a notification with the given title and message
     */
    fun showNotification(
        title: String,
        message: String,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        pendingIntent: PendingIntent? = null
    ) {
        if (!isNotificationEnabled()) {
            return
        }

        val builder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        pendingIntent?.let {
            builder.setContentIntent(it)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
            e.printStackTrace()
        }
    }

    /**
     * Shows a notification with custom small icon
     */
    fun showNotification(
        title: String,
        message: String,
        smallIcon: Int,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        pendingIntent: PendingIntent? = null
    ) {
        if (!isNotificationEnabled()) {
            return
        }

        val builder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        pendingIntent?.let {
            builder.setContentIntent(it)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels a specific notification by ID
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Updates an existing notification with new title and message
     */
    fun updateNotification(
        notificationId: Int,
        title: String,
        message: String,
        pendingIntent: PendingIntent? = null
    ) {
        showNotification(title, message, notificationId, pendingIntent)
    }

    /**
     * Updates notification with a custom notification object (for service notifications)
     */
    fun updateNotification(notificationId: Int, notification: android.app.Notification) {
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if notifications are enabled for the app
     */
    fun isNotificationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API 33) and above, check POST_NOTIFICATIONS permission
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED && notificationManager.areNotificationsEnabled()
        } else {
            // For older versions, just check if notifications are enabled
            notificationManager.areNotificationsEnabled()
        }
    }

    /**
     * Checks if a specific notification channel is enabled
     */
    fun isChannelEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = androidNotificationManager.getNotificationChannel(notificationChannelId)
            return channel?.importance != AndroidNotificationManager.IMPORTANCE_NONE
        }
        return true
    }

    /**
     * Requests notification permission for Android 13+ (API 33+)
     * This method should be called from an Activity
     */
    fun requestNotificationPermission(activity: FragmentActivity, callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val requestPermissionLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    callback(isGranted)
                }
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                callback(true)
            }
        } else {
            callback(true) // Permission not required for older versions
        }
    }

    /**
     * Alternative method to check if permission should be requested
     */
    fun shouldRequestNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    /**
     * Cancels all notifications from this app
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Creates a basic PendingIntent that opens the main activity
     */
    fun createMainActivityPendingIntent(activityClass: Class<*>): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /**
     * Shows a notification with progress bar
     */
    fun showProgressNotification(
        title: String,
        progress: Int,
        maxProgress: Int = 100,
        notificationId: Int = DEFAULT_NOTIFICATION_ID
    ) {
        if (!isNotificationEnabled()) {
            return
        }

        val builder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setProgress(maxProgress, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}