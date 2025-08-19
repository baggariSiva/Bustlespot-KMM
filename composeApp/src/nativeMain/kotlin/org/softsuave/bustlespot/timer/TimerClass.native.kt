package org.softsuave.bustlespot.timer

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import kotlin.random.Random
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.softsuave.bustlespot.locationmodule.LocationViewModel
import org.softsuave.bustlespot.tracker.ui.Coordinate
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
    actual var customeTimeForIdleTime: MutableStateFlow<Int> = MutableStateFlow(480)
    actual var numberOfScreenshot: MutableStateFlow<Int> = MutableStateFlow(1)
    actual var isTrackerStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val isTaskScheduled = atomic(false)


    private var curTimeCount: Int = 0
    private var locationRandomTime: Int = 0
    private var userCoordinate: Coordinate = Coordinate(0.0, 0.0)


    // Jobs for the idle and tracker coroutines.
    private var idleJob: Job? = null
    private var trackerJob: Job? = null
    private val screenShot = MutableStateFlow<ImageBitmap?>(null)
    actual val screenShotState: StateFlow<ImageBitmap?> = screenShot
    private val randomTime: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    private var trackerIndex = 0
    private val screenShotFrequency = 1
    private val screenshotLimit = 1
    private var idealStartTime: Instant = Instant.DISTANT_PAST
    private val postActivityInterval: Int = 60 //in second
    private val storeActivityInterval: Int = 60 //in second
    private val fetchLocationInterval: Int = 6 //in second

    actual fun resetTimer() {
        isTrackerRunning.value = false
        //    globalEventListener.unregisterListeners()
        idealTime.value = 0
        trackerTime.value = 0
    }

    actual fun getIdleTime(): Int {
        return (startTime.epochSeconds.seconds.inWholeSeconds - idealStartTime.epochSeconds.seconds.inWholeSeconds).toInt()
    }

    actual fun stopTimer() {
        Log.d("stopTimer")
        isTrackerRunning.value = false
        idealStartTime = Clock.System.now()
    }

    actual fun resumeTracker() {
        Log.d("resumeTracker")
        isTrackerRunning.value = true
    }

    actual fun startTimer() {
        isTrackerRunning.value = true
        isIdealTimerRunning.value = true
        trackerIndex = 0
        startTime = Clock.System.now()
        storeStartTime = Clock.System.now()
        locationRandomTime = Random.nextInt(0, postActivityInterval)
        print("Clicked on tracker button")
        Log.d("start Timer")
        // Idle timer coroutine (increments idealTime every second when active)
        viewModelScope.launch {
            locationViewModel.getUserCurrentLocation { it ->
                liveLocationCoordinate.value = it
                userCoordinate = it
            }
        }
        // Tracker timer coroutine (runs every second, checks for screenshot timing, etc.)
        if (!isTaskScheduled.getAndSet(true)) {
            trackerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000L) // wait 1 second
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
                                locationViewModel.getUserCurrentLocation { it ->
                                    liveLocationCoordinate.value = it
                                }
                        }
                        Log.d("$timeDifference and ${canCallApi.value}")
                        if (curTimeCount == locationRandomTime) {
                            locationViewModel.getUserCurrentLocation {coordinate->
                                userCoordinate = coordinate
                            }
                            Log.d("location set by random $userCoordinate")
                        }
                        trackerTime.value++
                        screenShotTakenTime.value++
                        curTimeCount++
                    }
                }
            }
        }
    }

    actual fun resetIdleTimer() {
        idealTime.value = 0
    }

    actual fun stopIdleTimer() {
        isIdealTimerRunning.value = false
        // globalEventListener.unregisterListeners()
    }

    actual fun startIdleTimerClock() {
        isIdealTimerRunning.value = true
    }

    fun takeScreenShot() {
        screenShot.value = org.softsuave.bustlespot.screenshot.takeScreenShot()
//        val bufferedImage: ImageBitmap? = screenShot.value
//        val file = File(System.getProperty("java.io.tmpdir"), "sampleFile")
//        ImageIO.write(bufferedImage, "png", file)
    }

    actual fun startScreenshotTask() {
//        if (screenshotRepeatingTask == null) {
//            screenshotRepeatingTask = object : TimerTask() {
//                override fun run() {
//                    if (!isPaused) {
//                        val randomDelay = Random.nextLong(0, 60 * 1000)
//                        screenshotOneShotTask?.cancel()
//                        screenshotOneShotTask = object : TimerTask() {
//                            override fun run() {
//                                takeScreenShot()
//                            }
//                        }
//                        timer.schedule(screenshotOneShotTask, randomDelay)
//                    }
//                }
//            }
//            timer.scheduleAtFixedRate(screenshotRepeatingTask, 0, 60 * 1000)
//        }
    }

    actual fun pauseScreenshotTask() {
//        isPaused = true
    }

    actual fun resumeScreenshotTask() {
//        isPaused = false
    }

    actual fun stopScreenshotTask() {
//        screenshotRepeatingTask?.cancel()
//        screenshotRepeatingTask = null
//        screenshotOneShotTask?.cancel()
//        screenshotOneShotTask = null
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
        base64Converter()
        val activity = ActivityData(
            startTime = startTime.toString(),
            endTime = Clock.System.now().toString(),
            billable = "",
            notes = "",
        )
        startTime = Clock.System.now()
        canCallApi.value = false
        return activity
    }

    actual fun getUntrackedActivityData(): ActivityData {
        base64Converter()
        val activity = ActivityData(
            startTime = idealStartTime.toString(),
            endTime = Clock.System.now().toString(),
            mouseActivity = 0,
            keyboardActivity = 0,
            totalActivity = 0,
            billable = "",
            notes = "",
        )
        startTime = Clock.System.now()
        mouseKeyEvents.value = 0
        keyboradKeyEvents.value = 0
        return activity
    }

    private fun base64Converter() {

//        screenShot.value?.toString()?.let { Log.d("this is great $it") }
//        viewModelScope.launch {
//            currentImageUri.value = screenShot.value?.let {
//                val byteArrayOutputStream = ByteArrayOutputStream()
//                //  ImageIO.write(it.toAwtImage(), "png", byteArrayOutputStream)
//                val bytes = byteArrayOutputStream.toByteArray()
//                Log.d("$bytes y")
//                Base64.getEncoder().encodeToString(bytes)
//            }.toString()
//        }


    }

    actual var canCallApi: MutableStateFlow<Boolean> = MutableStateFlow(false)

    //IOS not implemented
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
//            uri = currentImageUri.value
        )
        storeStartTime = Clock.System.now()

        return activity
    }

    actual fun updateStartTime() {
    }

    actual var liveLocationCoordinate: MutableStateFlow<Coordinate>  = MutableStateFlow(userCoordinate)

    actual fun getLocationData(): Coordinate? {
        return userCoordinate
    }
}
