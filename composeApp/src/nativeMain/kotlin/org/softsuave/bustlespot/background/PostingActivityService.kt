// iosMain/kotlin/org/softsuave/bustlespot/background/PostingActivityService.ios.kt
package org.softsuave.bustlespot.background

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.softsuave.bustlespot.utils.ActivityServiceState
import org.softsuave.bustlespot.tracker.data.TrackerRepository
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import org.softsuave.bustlespot.SessionManager
import com.example.Database
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.CoreLocation.*
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import kotlinx.cinterop.*
import platform.darwin.NSObject
import kotlin.native.concurrent.ThreadLocal
import kotlin.random.Random

// Separate the location delegate from the main service class
@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    var onLocationUpdate: ((CLLocation) -> Unit)? = null
    var onLocationError: ((NSError) -> Unit)? = null
    var onAuthorizationChange: ((CLAuthorizationStatus) -> Unit)? = null

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        @Suppress("UNCHECKED_CAST")
        val locations = didUpdateLocations as List<CLLocation>
        locations.lastOrNull()?.let { location ->
            onLocationUpdate?.invoke(location)
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onLocationError?.invoke(didFailWithError)
    }

    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        onAuthorizationChange?.invoke(didChangeAuthorizationStatus)
    }
}

@ThreadLocal
object PostingActivityServiceIOS : KoinComponent {

    private val sessionManager: SessionManager by inject()
    private val db: Database by inject()
    private val trackerRepository: TrackerRepository by inject()

    private val _currentState = MutableStateFlow(ActivityServiceState.STOPPED)
    val currentState: StateFlow<ActivityServiceState> = _currentState

    private var serviceJob: Job? = null
    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Timer variables
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var initialTime: Long = 0L
    private var serviceStartMillis: Long = 0L
    private var lastSendMillis: Long = 0L

    // Service-level task id & project id
    private var taskId: String? = null
    private var projectId: String? = null

    // Location manager and delegate
    private val locationManager = CLLocationManager()
    private val locationDelegate = LocationDelegate()
    private var currentLocation: CLLocation? = null

    // Send timer state
    private var sendElapsedMillis: Long = 0L
    private val sendIntervalMillis: Long = 10 * 60 * 1000L // 10 minutes
    private val sendTickMillis: Long = 1000L // 1 second tick

    // ISO date formatter function
    private fun formatToISO(timestampMillis: Long): String {
        val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
            timeZone = NSTimeZone.timeZoneWithName("UTC")!!
        }
        return formatter.stringFromDate(date)
    }

    // Background task identifier
    private var backgroundTaskId: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid

    init {
        setupLocationManager()
        setupNotifications()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupLocationManager() {
        locationManager.delegate = locationDelegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10.0 // Update every 10 meters

        // Setup delegate callbacks
        locationDelegate.onLocationUpdate = { location ->
            currentLocation = location
            NSLog("Location updated: ${location.coordinate.useContents { latitude }}, ${location.coordinate.useContents { longitude }}")
        }

        locationDelegate.onLocationError = { error ->
            NSLog("Location update failed: ${error.localizedDescription}")
        }

        locationDelegate.onAuthorizationChange = { status ->
            NSLog("Location authorization status changed to: $status")
            when (status) {
                kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> {
                    if (currentState.value == ActivityServiceState.STARTED) {
                        startLocationTracking()
                    }
                }
                kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                    NSLog("Location access denied")
                }
            }
        }
    }

    private fun setupNotifications() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (!granted) {
                NSLog("Notification permission not granted")
            }
        }
    }

    fun startService(
        postData: String? = null,
        taskId: String? = null,
        projectId: String? = null,
        initialTimeMillis: Long = 0L,
        onStart: () -> Unit = {}
    ) {
        if (currentState.value == ActivityServiceState.STARTED) {
            NSLog("Service already started")
            return
        }

        NSLog("Starting iOS posting service")
        _currentState.value = ActivityServiceState.STARTED

        this.taskId = taskId ?: generatePostId()
        this.projectId = projectId
        this.initialTime = initialTimeMillis

        startTime = getCurrentTimeMillis()
        pausedTime = 0L
        serviceStartMillis = startTime
        lastSendMillis = serviceStartMillis

        startBackgroundTask()
        requestLocationPermission()
        startTimerUpdates()
        startLocationTracking()

        showNotification("Activity Tracker Started", "Location tracking is now active")
        onStart.invoke()
    }

    fun stopService() {
        NSLog("Stopping iOS posting service")
        _currentState.value = ActivityServiceState.STOPPED

        timerJob?.cancel()
        locationJob?.cancel()
        serviceJob?.cancel()

        stopLocationTracking()
        endBackgroundTask()

        startTime = 0L
        pausedTime = 0L
        initialTime = 0L

        showNotification("Activity Tracker Stopped", "Location tracking has been stopped")
    }

    fun pauseService() {
        if (currentState.value != ActivityServiceState.STARTED) {
            NSLog("Cannot pause service in current state: ${currentState.value}")
            return
        }

        NSLog("Pausing iOS posting service")
        _currentState.value = ActivityServiceState.PAUSED

        pausedTime = getCurrentTimeMillis() - startTime
        timerJob?.cancel()
        locationJob?.cancel()

        showNotification("Activity Tracker Paused", "Location tracking is paused")
    }

    fun resumeService() {
        if (currentState.value != ActivityServiceState.PAUSED) {
            NSLog("Cannot resume service in current state: ${currentState.value}")
            return
        }

        NSLog("Resuming iOS posting service")
        _currentState.value = ActivityServiceState.STARTED

        initialTime += pausedTime
        startTime = getCurrentTimeMillis()
        pausedTime = 0L

        startTimerUpdates()
        startLocationTracking()

        showNotification("Activity Tracker Resumed", "Location tracking has resumed")
    }

    fun forceSendNow() {
        coroutineScope.launch {
            try {
                sendCurrentLocationOnce()
            } catch (e: Exception) {
                NSLog("forceSendNow failed: ${e.message}")
            } finally {
                sendElapsedMillis = 0L
            }
        }
    }

    private fun startBackgroundTask() {
        backgroundTaskId = UIApplication.sharedApplication.beginBackgroundTaskWithExpirationHandler {
            endBackgroundTask()
        }
    }

    private fun endBackgroundTask() {
        if (backgroundTaskId != UIBackgroundTaskInvalid) {
            UIApplication.sharedApplication.endBackgroundTask(backgroundTaskId)
            backgroundTaskId = UIBackgroundTaskInvalid
        }
    }

    private fun requestLocationPermission() {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestAlwaysAuthorization()
            }
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                NSLog("Location permission denied")
            }
            kCLAuthorizationStatusAuthorizedWhenInUse -> {
                locationManager.requestAlwaysAuthorization()
            }
            kCLAuthorizationStatusAuthorizedAlways -> {
                // Permission granted, start location updates
            }
        }
    }

    private fun startLocationTracking() {
        if (locationManager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways ||
            locationManager.authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse) {

            locationManager.startUpdatingLocation()

            // Start periodic location sending
            startLocationLoop()
        }
    }

    private fun stopLocationTracking() {
        locationManager.stopUpdatingLocation()
        locationJob?.cancel()
    }

    private fun startTimerUpdates() {
        timerJob = coroutineScope.launch {
            while (currentState.value == ActivityServiceState.STARTED) {
                val currentElapsed = getCurrentElapsedTime()
                val formattedTime = formatTime(currentElapsed)

                // Update notification periodically (every 30 seconds to avoid spam)
                if (currentElapsed % 30000 < 1000) {
                    showNotification(
                        "Activity Tracker - $formattedTime",
                        "Location tracking active"
                    )
                }

                delay(1000)
            }
        }
    }

    private fun startLocationLoop() {
        locationJob?.cancel()
        locationJob = coroutineScope.launch {
            // Initial send on start
            try {
                sendCurrentLocationOnce()
            } catch (e: Exception) {
                NSLog("Initial send failed: ${e.message}")
            }

            // Periodic send loop
            while (isActive && currentState.value == ActivityServiceState.STARTED) {
                delay(sendTickMillis)
                sendElapsedMillis += sendTickMillis

                if (sendElapsedMillis >= sendIntervalMillis) {
                    try {
                        sendCurrentLocationOnce()
                    } catch (e: Exception) {
                        NSLog("Periodic send failed: ${e.message}")
                    } finally {
                        sendElapsedMillis = 0L
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun sendCurrentLocationOnce() {
        var nowMillis = getCurrentTimeMillis()

        try {
            val location = currentLocation
            if (location == null) {
                NSLog("Location is null, skipping send")
                return
            }

            val tid = taskId ?: run {
                NSLog("No taskId available, skipping send")
                return
            }

            val startMillis = if (lastSendMillis > 0L) lastSendMillis else serviceStartMillis
            nowMillis = getCurrentTimeMillis()

            val startTimeStr = formatToISO(startMillis)
            val endTimeStr = formatToISO(nowMillis)

            val activityData = ActivityData(
                projectId = projectId,
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
                latitude = location.coordinate.useContents { latitude },
                longitude = location.coordinate.useContents { longitude },
                clickedKeys = null,
                lastScreenShotTime = null
            )

            trackerRepository.postUserActivity(activityData, isRetryCalls = false).collect { result ->
                when (result) {
                    is org.softsuave.bustlespot.auth.utils.Result.Loading -> {
                        NSLog("Posting location -> loading")
                    }
                    is org.softsuave.bustlespot.auth.utils.Result.Success -> {
                        NSLog("Posted activity successfully: ${result.data}")
                    }
                    is org.softsuave.bustlespot.auth.utils.Result.Error -> {
                        NSLog("Failed to post activity: ${result.message}")
                    }
                }
            }

        } catch (e: Exception) {
            NSLog("Error while obtaining/sending location: ${e.message}")
        } finally {
            lastSendMillis = nowMillis
            sendElapsedMillis = 0L
        }
    }

    private fun showNotification(title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(null) // Silent notification
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "location_tracking_${Random.nextInt()}",
            content = content,
            trigger = null // Immediate delivery
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            error?.let {
                NSLog("Failed to show notification: ${error.localizedDescription}")
            }
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    private fun generatePostId(): String = "post_${getCurrentTimeMillis()}"

    private fun getCurrentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    fun getServiceState(): ActivityServiceState = currentState.value

    fun getActiveTaskCount(): Int = if (currentState.value == ActivityServiceState.STARTED) 1 else 0

    fun getCurrentElapsedTime(): Long = when (currentState.value) {
        ActivityServiceState.STARTED -> initialTime + (getCurrentTimeMillis() - startTime)
        ActivityServiceState.PAUSED -> initialTime + pausedTime
        else -> initialTime
    }
}

actual class PostingActivityService actual constructor() {

    companion object {
        fun startService(
            postData: String? = null,
            taskId: String? = null,
            projectId: String? = null,
            initialTimeMillis: Long = 0L,
            onStart: () -> Unit = {}
        ) {
            PostingActivityServiceIOS.startService(
                postData = postData,
                taskId = taskId,
                projectId = projectId,
                initialTimeMillis = initialTimeMillis,
                onStart = onStart
            )
        }

        fun stopService() {
            PostingActivityServiceIOS.stopService()
        }

        fun pauseService() {
            PostingActivityServiceIOS.pauseService()
        }

        fun resumeService() {
            PostingActivityServiceIOS.resumeService()
        }

        fun sendNow() {
            PostingActivityServiceIOS.forceSendNow()
        }

        fun getInstance(): PostingActivityService? {
            // Return a wrapper or null based on service state
            return if (PostingActivityServiceIOS.getServiceState() != ActivityServiceState.STOPPED) {
                PostingActivityService()
            } else null
        }
    }

    fun getServiceState(): ActivityServiceState = PostingActivityServiceIOS.getServiceState()

    fun getActiveTaskCount(): Int = PostingActivityServiceIOS.getActiveTaskCount()

    fun getCurrentElapsedTime(): Long = PostingActivityServiceIOS.getCurrentElapsedTime()

    val currentState: StateFlow<ActivityServiceState> = PostingActivityServiceIOS.currentState
}