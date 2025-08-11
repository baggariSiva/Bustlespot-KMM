package org.softsuave.bustlespot.background

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.softsuave.bustlespot.R
import org.softsuave.bustlespot.utils.ActivityServiceState
import org.koin.android.ext.android.inject
import org.softsuave.bustlespot.tracker.data.TrackerRepository
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import org.softsuave.bustlespot.SessionManager
import com.example.Database
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_PROJECT_ID = "extra_project_id" // <-- new extra for projectId

        // Action constants
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"
        const val ACTION_SEND_NOW = "action_send_now" // immediate send

        private var serviceInstance: PostingActivityService? = null

        @Volatile
        private var lastSendMillis: Long = 0L

        fun startService(
            context: Context,
            postData: String? = null,
            taskId: String? = null,
            projectId: String? = null,                 // <-- accept projectId
            initialTimeMillis: Long = 0L,
            onStart: () -> Unit = {}
        ) {
            val intent = Intent(context, PostingActivityService::class.java).apply {
                action = ActivityServiceState.STARTED.toString()
                postData?.let { putExtra(EXTRA_POST_DATA, it) }
                taskId?.let { putExtra(EXTRA_TASK_ID, it) }
                projectId?.let { putExtra(EXTRA_PROJECT_ID, it) } // <-- put projectId
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

        fun sendNow(context: Context) {
            val intent = Intent(context, PostingActivityService::class.java).apply {
                action = ACTION_SEND_NOW
            }
            context.startService(intent)
        }

        fun getInstance(): PostingActivityService? = serviceInstance
    }

    // ---------- KOIN injections ----------
    private val sessionManager: SessionManager by inject()
    private val db: Database by inject()
    private val httpClient: Any by inject() // keep generic; Koin provides HttpClient if needed
    private val trackerRepository: TrackerRepository by inject()
    // -------------------------------------

    private val _currentState = MutableStateFlow(ActivityServiceState.STOPPED)
    val currentState: StateFlow<ActivityServiceState> = _currentState

    private var serviceJob: Job? = null
    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Timer variables for notification (existing)
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var initialTime: Long = 0L

    // Active posting tasks
    private val activePostingTasks = ConcurrentHashMap<String, Job>()

    // Notification manager wrapper (assumed available in your project)
    private lateinit var notificationManager: NotificationManager

    // Fused location client
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // Service-level task id & project id (from Intent)
    private var taskId: String? = null
    private var projectId: String? = null // <-- new variable

    // When service started (used as start_time)
    private var serviceStartMillis: Long = 0L

    // ISO formatter for start_time/end_time (UTC)
    private val isoFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // -------------------- send-timer state (separate from notification timer) --------------------
    @Volatile
    private var sendElapsedMillis: Long = 0L

    private val sendIntervalMillis: Long = 10 * 60 * 1000L // 10 minutes
    private val sendTickMillis: Long = 1000L // 1 second tick granularity
    // ---------------------------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        serviceInstance = this

        notificationManager = NotificationManager(
            context = this,
            notificationChannelId = CHANNEL_ID,
            notificationChannelName = "Posting Service",
            notificationChannelDescription = "Background posting operations"
        )
        notificationManager.createNotificationChannel()

        Log.d(TAG, "Koin-injected SessionManager available? ${sessionManager != null}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "Service command received: $action")

        when (action) {
            ActivityServiceState.STARTED.toString(), ACTION_START -> handleStarted(intent)
            ActivityServiceState.STOPPED.toString(), ACTION_STOP -> handleStopped()
            ActivityServiceState.PAUSED.toString(), ACTION_PAUSE -> handlePaused()
            ActivityServiceState.RESUMED.toString(), ACTION_RESUME -> handleResumed()
            ACTION_SEND_NOW -> {
                Log.d(TAG, "Immediate send requested via ACTION_SEND_NOW")
                forceSendNow()
            }
            else -> Log.w(TAG, "Unknown action received: $action")
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

        initialTime = intent?.getLongExtra(EXTRA_INITIAL_TIME, 0L) ?: 0L
        taskId = intent?.getStringExtra(EXTRA_TASK_ID) ?: generatePostId()
        projectId = intent?.getStringExtra(EXTRA_PROJECT_ID) // <-- read projectId from intent
        Log.d(TAG, "Tracking for taskId: $taskId, projectId: $projectId")

        startTime = System.currentTimeMillis()
        pausedTime = 0L
        serviceStartMillis = startTime

        lastSendMillis = serviceStartMillis

        startForegroundService()
        startTimerUpdates()
        startLocationLoop()

        val postData = intent?.getStringExtra(EXTRA_POST_DATA)
        val postId = intent?.getStringExtra(EXTRA_POST_ID) ?: generatePostId()
        postData?.let { data -> startPostingWork(data, postId) }
    }

    private fun handleStopped() {
        Log.d(TAG, "Stopping posting service")
        _currentState.value = ActivityServiceState.STOPPED

        timerJob?.cancel()
        locationJob?.cancel()
        cancelAllPostingTasks()
        serviceJob?.cancel()

        startTime = 0L
        pausedTime = 0L
        initialTime = 0L

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

        pausedTime = System.currentTimeMillis() - startTime

        timerJob?.cancel()
        locationJob?.cancel()

        pausePostingTasks()
        updateNotificationForPausedState()
    }

    private fun handleResumed() {
        if (currentState.value != ActivityServiceState.PAUSED) {
            Log.w(TAG, "Cannot resume service in current state: $currentState")
            return
        }

        Log.d(TAG, "Resuming posting service")
        _currentState.value = ActivityServiceState.STARTED

        initialTime += pausedTime
        startTime = System.currentTimeMillis()
        pausedTime = 0L

        startTimerUpdates()
        startLocationLoop()
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
                delay(1000)
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

        val startIntent = PendingIntent.getService(this, 0, Intent(this, PostingActivityService::class.java).apply { action = ACTION_START }, flags)
        val pauseIntent = PendingIntent.getService(this, 1, Intent(this, PostingActivityService::class.java).apply { action = ACTION_PAUSE }, flags)
        val resumeIntent = PendingIntent.getService(this, 2, Intent(this, PostingActivityService::class.java).apply { action = ACTION_RESUME }, flags)
        val stopIntent = PendingIntent.getService(this, 3, Intent(this, PostingActivityService::class.java).apply { action = ACTION_STOP }, flags)
        val sendNowIntent = PendingIntent.getService(this, 4, Intent(this, PostingActivityService::class.java).apply { action = ACTION_SEND_NOW }, flags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity Tracker - $timeText")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.notification_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\nElapsed Time: $timeText"))
            .setSound(null)
            .setVibrate(null)

        when (currentState.value) {
            ActivityServiceState.STOPPED -> builder.addAction(android.R.drawable.ic_media_play, "Start", startIntent)
            ActivityServiceState.STARTED -> {
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
                builder.addAction(android.R.drawable.ic_menu_upload, "Send Now", sendNowIntent) // manual send action
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            }
            ActivityServiceState.PAUSED -> {
                builder.addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            }
            else -> builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
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
                val postingTask = launch { performPostingOperation(postData, postId) }
                activePostingTasks[postId] = postingTask
                postingTask.join()
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
            while (currentState.value == ActivityServiceState.PAUSED) delay(1000)
            if (currentState.value == ActivityServiceState.STOPPED) throw CancellationException("Service stopped")
            delay(3000) // simulate
            sendPostingResult(postId, success = true, message = "Post completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete posting operation", e)
            throw e
        }
    }

    private fun cancelAllPostingTasks() {
        Log.d(TAG, "Cancelling all posting tasks")
        activePostingTasks.values.forEach { it.cancel() }
        activePostingTasks.clear()
    }

    private fun pausePostingTasks() {
        Log.d(TAG, "Pausing ${activePostingTasks.size} posting tasks")
    }

    private fun resumePostingTasks() {
        Log.d(TAG, "Resuming ${activePostingTasks.size} posting tasks")
    }

    private fun handlePostingError(postId: String, error: Exception) {
        Log.e(TAG, "Posting error for ID: $postId", error)
        sendPostingResult(postId, success = false, message = error.message ?: "Unknown error")
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

    private fun generatePostId(): String = "post_${System.currentTimeMillis()}"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        serviceInstance = null
        timerJob?.cancel()
        locationJob?.cancel()
        cancelAllPostingTasks()
        serviceJob?.cancel()
        coroutineScope.cancel()
    }

    fun getServiceState(): ActivityServiceState = currentState.value
    fun getActiveTaskCount(): Int = activePostingTasks.size
    fun getCurrentElapsedTime(): Long = when (currentState.value) {
        ActivityServiceState.STARTED -> initialTime + (System.currentTimeMillis() - startTime)
        ActivityServiceState.PAUSED -> initialTime + pausedTime
        else -> initialTime
    }

    // -----------------------------
    // Location loop & posting using TrackerRepository.postUserActivity(...)
    // -----------------------------
    @SuppressLint("MissingPermission")
    private fun startLocationLoop() {
        locationJob?.cancel()
        locationJob = coroutineScope.launch {
            // immediate send on start (do not block service start if it fails)
            try {
                sendCurrentLocationOnce()
            } catch (e: Exception) {
                Log.e(TAG, "Initial send failed: ${e.message}", e)
            }

            // tick loop: update sendElapsedMillis every second and trigger send when >= interval
            while (isActive && currentState.value == ActivityServiceState.STARTED) {
                delay(sendTickMillis)
                sendElapsedMillis += sendTickMillis

                // if you want to show countdown in notification, call updateNotificationForRunningState(...)
                if (sendElapsedMillis >= sendIntervalMillis) {
                    try {
                        sendCurrentLocationOnce()
                    } catch (e: Exception) {
                        Log.e(TAG, "Periodic send failed: ${e.message}", e)
                    } finally {
                        sendElapsedMillis = 0L
                    }
                }
            }
        }
    }

    /**
     * Force send now from outside (resets the send timer to 0)
     */
    fun forceSendNow() {
        coroutineScope.launch {
            try {
                sendCurrentLocationOnce()
            } catch (e: Exception) {
                Log.e(TAG, "forceSendNow failed: ${e.message}", e)
            } finally {
                sendElapsedMillis = 0L
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendCurrentLocationOnce() {
        var nowMillis: Long = System.currentTimeMillis()
        try {
            val location = getLastLocation()
            if (location == null) {
                Log.w(TAG, "Location is null, skipping send")
                return
            }

            val tid = taskId ?: run {
                Log.w(TAG, "No taskId available, skipping send")
                return
            }

            // If lastSendMillis is 0 (shouldn't be), fallback to serviceStartMillis
            val startMillis = if (lastSendMillis > 0L) lastSendMillis else serviceStartMillis
            nowMillis = System.currentTimeMillis() // mark actual end time

            val startTimeStr = isoFormatter.format(Date(startMillis))
            val endTimeStr = isoFormatter.format(Date(nowMillis))

            // Build ActivityData to reuse repository's postUserActivity logic.
            val activityData = ActivityData(
                projectId = projectId, // may be null
                taskId = tid,
                startTime = startTimeStr,
                endTime = endTimeStr,
                mouseActivity = 0,
                keyboardActivity = 0,
                totalActivity = 0,
                notes = null,
                orgId = null,
                uri = emptyList(),
                unTrackedTime = null,
                latitude = location.latitude,
                longitude = location.longitude,
                clickedKeys = null,
                lastScreenShotTime = null
            )

            // collect the repo flow and log results (this will emit Loading => Success/Error)
            trackerRepository.postUserActivity(activityData, isRetryCalls = false).collect { result ->
                when (result) {
                    is org.softsuave.bustlespot.auth.utils.Result.Loading -> {
                        Log.d(TAG, "Posting location -> loading")
                    }
                    is org.softsuave.bustlespot.auth.utils.Result.Success -> {
                        Log.d(TAG, "Posted activity successfully: ${result.data}")
                    }
                    is org.softsuave.bustlespot.auth.utils.Result.Error -> {
                        Log.e(TAG, "Failed to post activity: ${result.message}")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error while obtaining/sending location: ${e.message}", e)
        } finally {
            // update lastSendMillis to now so next send's start_time is this send's end_time
            lastSendMillis = nowMillis
            // reset send timer after attempting a send
            sendElapsedMillis = 0L
        }
    }


    /**
     * Safely get last location with runtime permission check.
     * Returns null if permissions are not granted or location is unavailable.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val task = fusedLocationClient.lastLocation
            task.addOnSuccessListener { loc ->
                if (cont.isActive) cont.resume(loc)
            }
            task.addOnFailureListener { ex ->
                if (cont.isActive) cont.resumeWithException(ex)
            }
            cont.invokeOnCancellation {
                // nothing to explicitly cancel on the Task API - listeners are GC'd
            }
        } catch (se: SecurityException) {
            if (cont.isActive) cont.resumeWithException(se)
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
}
