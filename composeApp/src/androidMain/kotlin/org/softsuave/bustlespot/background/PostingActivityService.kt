package org.softsuave.bustlespot.background

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.softsuave.bustlespot.R
import org.softsuave.bustlespot.utils.ActivityServiceState
import java.util.*
import java.util.concurrent.ConcurrentHashMap

actual class PostingActivityService actual constructor() : Service() {

    companion object {
        private const val TAG = "PostingActivityService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "posting_service_channel"

        // Intent extras
        const val EXTRA_POST_DATA = "extra_post_data"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_POST_TYPE = "extra_post_type"
        const val EXTRA_INITIAL_TIME = "extra_initial_time"

        // Action constants
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"

        // Static instance to access service methods from common code
        private var serviceInstance: PostingActivityService? = null

        // Helper methods to control the service
        fun startService(context: Context, postData: String? = null, initialTimeMillis: Long = 0L,
                         onStart: () -> Unit = {}) {
            val intent = Intent(context, PostingActivityService::class.java).apply {
                action = ActivityServiceState.STARTED.toString()
                postData?.let { putExtra(EXTRA_POST_DATA, it) }
                putExtra(EXTRA_INITIAL_TIME, initialTimeMillis)
            }
            context.startForegroundService(intent)
            onStart.invoke()
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PostingActivityService::class.java).apply {
                action = ActivityServiceState.STOPPED.toString()
            }
            context.startService(intent)
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, PostingActivityService::class.java).apply {
                action = ActivityServiceState.PAUSED.toString()
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, PostingActivityService::class.java).apply {
                action = ActivityServiceState.RESUMED.toString()
            }
            context.startService(intent)
        }

        // Method to get service instance (useful for common code access)
        fun getInstance(): PostingActivityService? = serviceInstance
    }

    private val _currentState = MutableStateFlow(ActivityServiceState.STOPPED)
    val currentState: StateFlow<ActivityServiceState> = _currentState
    private var serviceJob: Job? = null
    private var timerJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Timer related variables
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var pausedTime: Long = 0L
    private var initialTime: Long = 0L

    // Store active posting tasks
    private val activePostingTasks = ConcurrentHashMap<String, Job>()

    // Notification manager
    private lateinit var notificationManager: NotificationManager

    fun getServiceState(): ActivityServiceState = currentState.value

    fun getActiveTaskCount(): Int = activePostingTasks.size

    fun getCurrentElapsedTime(): Long = when (currentState.value) {
        ActivityServiceState.STARTED -> initialTime + (System.currentTimeMillis() - startTime)
        ActivityServiceState.PAUSED -> initialTime + pausedTime
        else -> initialTime
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        serviceInstance = this

        // Initialize notification manager
        notificationManager = NotificationManager(
            context = this,
            notificationChannelId = CHANNEL_ID,
            notificationChannelName = "Posting Service",
            notificationChannelDescription = "Background posting operations"
        )

        // Create notification channel
        notificationManager.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "Service command received: $action")

        when (action) {
            ActivityServiceState.STARTED.toString() -> {
                handleStarted(intent)
            }
            ActivityServiceState.STOPPED.toString(), ACTION_STOP -> {
                handleStopped()
            }
            ActivityServiceState.PAUSED.toString(), ACTION_PAUSE -> {
                handlePaused()
            }
            ActivityServiceState.RESUMED.toString(), ACTION_RESUME -> {
                handleResumed()
            }
            ACTION_START -> {
                handleStarted(intent)
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action")
            }
        }

        return START_STICKY
    }

    private fun handleStarted(intent: Intent?) {
        if (currentState.value == ActivityServiceState.STARTED) {
            Log.d(TAG, "Service already started")
            return
        }

        _currentState.value = ActivityServiceState.STARTED
        Log.d(TAG, "Starting posting service")

        // Initialize timer
        initialTime = intent?.getLongExtra(EXTRA_INITIAL_TIME, 0L) ?: 0L


        print("Initial time: $initialTime")

        startTime = System.currentTimeMillis()
        pausedTime = 0L

        // Start as foreground service
        startForegroundService()

        // Start timer update job
        startTimerUpdates()

        // Extract post data from intent
        val postData = intent?.getStringExtra(EXTRA_POST_DATA)
        val postId = intent?.getStringExtra(EXTRA_POST_ID) ?: generatePostId()

        // Start background posting work if data is provided
        postData?.let { data ->
            startPostingWork(data, postId)
        }
    }

    private fun handleStopped() {
        Log.d(TAG, "Stopping posting service")
        _currentState.value = ActivityServiceState.STOPPED

        // Stop timer
        timerJob?.cancel()

        // Cancel all active posting tasks
        cancelAllPostingTasks()

        // Cancel the main service job
        serviceJob?.cancel()

        // Reset timer variables
        startTime = 0L
        elapsedTime = 0L
        pausedTime = 0L
        initialTime = 0L

        // Stop foreground service and remove notification
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handlePaused() {
        if (currentState.value != ActivityServiceState.STARTED) {
            Log.w(TAG, "Cannot pause service in current state: $currentState")
            return
        }

        Log.d(TAG, "Pausing posting service")
        _currentState.value = ActivityServiceState.PAUSED

        // Calculate paused time
        pausedTime = System.currentTimeMillis() - startTime

        // Stop timer updates
        timerJob?.cancel()

        // Pause ongoing operations
        pausePostingTasks()

        // Update notification to show paused state
        updateNotificationForPausedState()
    }

    private fun handleResumed() {
        if (currentState.value != ActivityServiceState.PAUSED) {
            Log.w(TAG, "Cannot resume service in current state: $currentState")
            return
        }

        Log.d(TAG, "Resuming posting service")
        _currentState.value = ActivityServiceState.STARTED

        // Update initial time with paused duration
        initialTime += pausedTime
        startTime = System.currentTimeMillis()
        pausedTime = 0L

        // Resume timer updates
        startTimerUpdates()

        // Resume paused operations
        resumePostingTasks()
    }

    private fun startTimerUpdates() {
        timerJob = coroutineScope.launch {
            while (currentState.value == ActivityServiceState.STARTED) {
                val currentElapsed = getCurrentElapsedTime()
                val formattedTime = formatTime(currentElapsed)

                withContext(Dispatchers.Main) {
                    updateNotificationForRunningState(formattedTime)
                }

                delay(1000) // Update every second
            }
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("00:00:00", "Starting...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationForRunningState(formattedTime: String) {
        val notification = createNotification(formattedTime, "Active - ${activePostingTasks.size} tasks running")
        notificationManager.updateNotification(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationForPausedState() {
        val formattedTime = formatTime(getCurrentElapsedTime())
        val notification = createNotification(formattedTime, "Paused - ${activePostingTasks.size} tasks paused")
        notificationManager.updateNotification(NOTIFICATION_ID, notification)
    }

    private fun createNotification(timeText: String, statusText: String): android.app.Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val startIntent = PendingIntent.getService(
            this, 0,
            Intent(this, PostingActivityService::class.java).apply { action = ACTION_START },
            flags
        )

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PostingActivityService::class.java).apply { action = ACTION_PAUSE },
            flags
        )

        val resumeIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PostingActivityService::class.java).apply { action = ACTION_RESUME },
            flags
        )

        val stopIntent = PendingIntent.getService(
            this, 3,
            Intent(this, PostingActivityService::class.java).apply { action = ACTION_STOP },
            flags
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity Tracker - $timeText")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.notification_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\nElapsed Time: $timeText"))
            .setSound(null)
            .setVibrate(null)

        // Add action buttons based on current state
        when (currentState.value) {
            ActivityServiceState.STOPPED -> {
                builder.addAction(android.R.drawable.ic_media_play, "Start", startIntent)
            }
            ActivityServiceState.STARTED -> {
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            }
            ActivityServiceState.PAUSED -> {
                builder.addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            }
            else -> {
                // Default actions
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            }
        }

        return builder.build()
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun startPostingWork(postData: String, postId: String) {
        serviceJob = coroutineScope.launch {
            try {
                Log.d(TAG, "Starting posting work for ID: $postId")

                // Create and start a posting task
                val postingTask = launch {
                    performPostingOperation(postData, postId)
                }

                // Store the task for management
                activePostingTasks[postId] = postingTask

                // Wait for completion
                postingTask.join()

                // Remove completed task
                activePostingTasks.remove(postId)

                Log.d(TAG, "Posting work completed for ID: $postId")

            } catch (e: Exception) {
                Log.e(TAG, "Error in posting work", e)
                handlePostingError(postId, e)
            }
        }
    }

    private suspend fun performPostingOperation(postData: String, postId: String) {
        Log.d(TAG, "Performing posting operation for: $postId")

        try {
            // Check if service is paused
            while (currentState.value == ActivityServiceState.PAUSED) {
                delay(1000) // Wait while paused
            }

            // If stopped, cancel operation
            if (currentState.value == ActivityServiceState.STOPPED) {
                throw CancellationException("Service stopped")
            }

            // TODO: Replace with your actual posting logic
            // Example implementation:
            // 1. Parse postData (ActivityData)
            // 2. Upload to server
            // 3. Handle response
            delay(3000) // Simulate network operation

            // Send success result
            sendPostingResult(postId, success = true, message = "Post completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete posting operation", e)
            throw e
        }
    }

    private fun cancelAllPostingTasks() {
        Log.d(TAG, "Cancelling all posting tasks")
        activePostingTasks.values.forEach { job ->
            job.cancel()
        }
        activePostingTasks.clear()
    }

    private fun pausePostingTasks() {
        Log.d(TAG, "Pausing ${activePostingTasks.size} posting tasks")
        // Tasks will automatically pause due to state check in performPostingOperation
    }

    private fun resumePostingTasks() {
        Log.d(TAG, "Resuming ${activePostingTasks.size} posting tasks")
        // Tasks will automatically resume when state changes
    }

    private fun handlePostingError(postId: String, error: Exception) {
        Log.e(TAG, "Posting error for ID: $postId", error)

        // Send error result
        sendPostingResult(postId, success = false, message = error.message ?: "Unknown error")

        // Remove failed task
        activePostingTasks.remove(postId)
    }

    private fun sendPostingResult(postId: String, success: Boolean, message: String) {
        val intent = Intent("org.softsuave.bustlespot.POSTING_RESULT").apply {
            putExtra("post_id", postId)
            putExtra("success", success)
            putExtra("message", message)
            putExtra("elapsed_time", getCurrentElapsedTime())
        }
        sendBroadcast(intent)
    }

    private fun generatePostId(): String {
        return "post_${System.currentTimeMillis()}"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        serviceInstance = null

        // Clean up resources
        timerJob?.cancel()
        cancelAllPostingTasks()
        serviceJob?.cancel()
        coroutineScope.cancel()
    }
}

