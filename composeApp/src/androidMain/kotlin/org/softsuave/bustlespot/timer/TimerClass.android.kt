package org.softsuave.bustlespot.timer

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.locationmodule.LocationViewModel
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import org.softsuave.bustlespot.tracker.ui.Coordinate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Int
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.seconds

actual class TrackerModule actual constructor(
    private val viewModelScope: CoroutineScope,
    private val locationViewModel: LocationViewModel
) {
    actual var trackerTime: MutableStateFlow<Int> = MutableStateFlow(0)
    actual var isTrackerRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    actual var isIdealTimerRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    actual var idealTime: MutableStateFlow<Int> = MutableStateFlow(0)
    actual var screenShotTakenTime: MutableStateFlow<Int> = MutableStateFlow(0)
    actual var keyboradKeyEvents: MutableStateFlow<Int> = MutableStateFlow(0)
    actual var mouseKeyEvents: MutableStateFlow<Int> = MutableStateFlow(0)
    actual var mouseMotionCount: MutableStateFlow<Int> = MutableStateFlow(0)
    actual var customeTimeForIdleTime: MutableStateFlow<Int> = MutableStateFlow(40)
    actual var numberOfScreenshot: MutableStateFlow<Int> = MutableStateFlow(1)
    actual var isTrackerStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val currentImageUri: MutableStateFlow<String> = MutableStateFlow("")
    private var timer = Timer()
    private var isTaskScheduled = AtomicBoolean(false)
    private var isIdleTaskScheduled = AtomicBoolean(false)
    private val screenShot = MutableStateFlow<ImageBitmap?>(null)
    actual val screenShotState: StateFlow<ImageBitmap?> = screenShot
    private val randomTime: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    private var screenshotRepeatingTask: TimerTask? = null
    private var screenshotOneShotTask: TimerTask? = null
    private var isObservingEvents: Boolean = false

    private var curTimeCount: Int = 0
    private var locationRandomTime: Int = 0
    private var userCoordinate: Coordinate = Coordinate(0.0, 0.0)

    @Volatile
    private var isPaused = false

    private var trackerTimerTask: TimerTask? = null
    private var idleTimerTask: TimerTask? = null
    private var trackerIndex = 0
    private val screenShotFrequency = 10
    private val screenshotLimit = 1
    private var idealStartTime: Instant = Instant.DISTANT_PAST
    private val postActivityInterval: Int = 600 //in second
    private val storeActivityInterval: Int = 60 //in second
    private val fetchLocationInterval: Int = 60 //in second
    actual fun resetTimer() {
        isTrackerRunning.value = false
        Log.d("idle time rested")
        trackerTime.value = 0
    }

    actual fun stopTimer() {
        Log.d("stopTimer")
        isTrackerRunning.value = false
    }

    actual fun resumeTracker() {
        Log.d("resumeTracker")
        isTrackerRunning.value = true
    }

    actual fun startTimer() {
        isTrackerRunning.value = true
        isIdealTimerRunning.value = true
        startTime = Clock.System.now()
        storeStartTime = Clock.System.now()
        locationRandomTime = Random.nextInt(0, postActivityInterval)
        viewModelScope.launch {
            locationViewModel.getUserCurrentLocation { it ->
                liveLocationCoordinate.value = it
            }
        }
        if (!isTaskScheduled.getAndSet(true)) {
            trackerTimerTask = object : TimerTask() {
                override fun run() {
                    if (isTrackerRunning.value) {
                        val currentTime = Clock.System.now()
                        val timeDifference = currentTime.epochSeconds - startTime.epochSeconds
                        val storeTimeDifference =
                            currentTime.epochSeconds - storeStartTime.epochSeconds
                        if (timeDifference >= postActivityInterval) {
                            canCallApi.value = true
                            curTimeCount = 0
                            locationRandomTime = Random.nextInt(0, postActivityInterval)
                        }
                        if (curTimeCount % fetchLocationInterval == 0) {
                            viewModelScope.launch {
                                locationViewModel.getUserCurrentLocation { it ->
                                    liveLocationCoordinate.value = it
                                }
                            }
                        }
                        if (storeTimeDifference >= storeActivityInterval) {
                            canStoreApiCall.value = true
                        }
                        Log.d("$timeDifference and ${canCallApi.value}")
                        trackerTime.value++ // need to add initial ideal time
                        curTimeCount = curTimeCount.inc()
                        if (curTimeCount == locationRandomTime) {
                            viewModelScope.launch {
                                locationViewModel.getUserCurrentLocation {coordinate->
                                    userCoordinate = coordinate
                                }
                            }
                            Log.d("location set by random $userCoordinate")
                        }
                        println("Current minute: ${(trackerTime.value % 3600) / 60}")
                        println("Random times: ${randomTime.value}")
                    }
                }
            }
            timer.schedule(trackerTimerTask, 1000, 1000)
        }
    }


    actual fun resetIdleTimer() {
        idealTime.value = 0
        Log.d("idle time rested")
    }

    actual fun stopIdleTimer() {
        isIdealTimerRunning.value = false
        // globalEventListener.unregisterListeners()
    }

    actual fun startIdleTimerClock() {
        isIdealTimerRunning.value = true
    }

    actual fun getIdleTime(): Int {
        return (startTime.epochSeconds.seconds.inWholeSeconds - idealStartTime.epochSeconds.seconds.inWholeSeconds).toInt()
    }

    fun takeScreenShot() {
        screenShot.value = org.softsuave.bustlespot.screenshot.takeScreenShot()
        screenShot.value?.let { saveImageAndConvertToBase64(it) }?.let {
            currentImageUri.value = it
        }
    }

    actual fun startScreenshotTask() {
    }

    actual fun pauseScreenshotTask() {

    }

    actual fun resumeScreenshotTask() {

    }

    actual fun stopScreenshotTask() {
    }

    actual fun updateTrackerTimer() {
        val newTime = trackerTime.value - customeTimeForIdleTime.value
        trackerTime.value = if (newTime < 0) 0 else newTime
    }

    actual fun startIdleTimer() {
    }

    actual fun addCustomTimeForIdleTime(time: Int) {
        customeTimeForIdleTime.value = time
    }

    actual fun setTrackerTime(trackerTime: Int, idealTime: Int) {
        this.trackerTime.value = trackerTime
        // this.idealTime.value = idealTime
    }

    actual fun setLastScreenShotTime(time: Int) {
        screenShotTakenTime.value = time
    }

    actual var startTime: Instant = Instant.DISTANT_FUTURE


    actual fun getActivityData(): ActivityData {
        val endTime = Clock.System.now()
        val activity = ActivityData(
            startTime = startTime.toString(),
            endTime = endTime.toString(),
        )
        startTime = endTime
        canCallApi.value = false
        return activity
    }

    actual fun getUntrackedActivityData(): ActivityData {
        val activity = ActivityData(
            startTime = idealStartTime.toString(),
            endTime = Clock.System.now().toString(),
            mouseActivity = 0,
            keyboardActivity = 0,
            totalActivity = 0,
            billable = "",
            notes = "",
            unTrackedTime = idealTime.value.toLong(),
        )
        startTime = Clock.System.now()
        mouseKeyEvents.value = 0
        keyboradKeyEvents.value = 0
        return activity
    }

    fun saveImageAndConvertToBase64(imageBitmap: ImageBitmap): String {
        val bitmap: Bitmap = imageBitmap.asAndroidBitmap()

        val tempFile = File.createTempFile("sampleFile", ".png")

        FileOutputStream(tempFile).use { fileOutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        }

        ByteArrayOutputStream().use { byteArrayOutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
        }
    }

    actual var canCallApi: MutableStateFlow<Boolean> = MutableStateFlow(false)
    actual var canStoreApiCall: MutableStateFlow<Boolean> = MutableStateFlow(false)
    actual var storeStartTime: Instant
        get() = Instant.DISTANT_PAST
        set(value) {}

    actual fun getStoreActivityData(): ActivityData {
        val activity = ActivityData(
            startTime = storeStartTime.toString(),
            endTime = Clock.System.now().toString(),
            mouseActivity = mouseKeyEvents.value,
            keyboardActivity = keyboradKeyEvents.value,
            totalActivity = (mouseKeyEvents.value + keyboradKeyEvents.value) % 100,
            billable = "",
            notes = "",
        )
        storeStartTime = Clock.System.now()
        return activity
    }

    actual fun updateStartTime() {
    }


    actual var liveLocationCoordinate: MutableStateFlow<Coordinate> =
        MutableStateFlow(userCoordinate)

    actual fun getLocationData(): Coordinate? {
        return userCoordinate
    }


}
