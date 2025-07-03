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
import kotlin.time.Duration.Companion.seconds

actual class TrackerModule actual constructor(private val viewModelScope: CoroutineScope) {
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

    private val currentImageUri: MutableStateFlow<String> = MutableStateFlow("")
    //    private var timer = Timer()
    private val isIdleTaskScheduled = atomic(false)
    private val isTaskScheduled = atomic(false)

    // Jobs for the idle and tracker coroutines.
    private var idleJob: Job? = null
    private var trackerJob: Job? = null
    private val screenShot = MutableStateFlow<ImageBitmap?>(null)
    actual val screenShotState: StateFlow<ImageBitmap?> = screenShot
    private val randomTime: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
//    private var screenshotRepeatingTask: TimerTask? = null
//    private var screenshotOneShotTask: TimerTask? = null


//    @Volatile
//    private var isPaused = false
//
//    private var trackerTimerTask: TimerTask? = null
//    private var idleTimerTask: TimerTask? = null
    private var trackerIndex = 0
    private val screenShotFrequency = 1
    private val screenshotLimit = 1
    private var idealStartTime: Instant = Instant.DISTANT_PAST
    private val postActivityInterval: Int = 600 //in second

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
        //   globalEventListener.unregisterListeners()
    }

    actual fun resumeTracker() {
        Log.d("resumeTracker")
        isTrackerRunning.value = true
        //  globalEventListener.registerListeners()
    }

    private fun setRandomTimes(
        randomTimes: MutableStateFlow<List<Int>>,
        overallStart: Int,
        overallEnd: Int,
        numberOfIntervals: Int = 1
    ) {
        val totalDuration = overallEnd - overallStart
        if (totalDuration % numberOfIntervals != 0) {
            throw IllegalArgumentException("Interval length ($totalDuration) must be evenly divisible by $numberOfIntervals.")
        }
        val intervalSize = totalDuration / numberOfIntervals
        randomTimes.value = List(numberOfIntervals) { i ->
            val subIntervalStart = overallStart + i * intervalSize
            val subIntervalEnd = overallStart + (i + 1) * intervalSize
            Random.nextInt(from = subIntervalStart, until = subIntervalEnd)
        }
    }

    actual fun startTimer() {
        isTrackerRunning.value = true
        isIdealTimerRunning.value = true
        //    globalEventListener.registerListeners()
        setRandomTimes(
            randomTime,
            overallStart = 0,
            overallEnd = screenshotLimit * 60,
            numberOfIntervals = screenShotFrequency
        )
        trackerIndex = 0
        viewModelScope.launch {
//            GlobalAccessibilityEvents.keyCountFlow.collectLatest { count ->
//                keyboradKeyEvents.emit(count)
//                idealTime.value = 0
//            }
        }
        viewModelScope.launch {
//            GlobalAccessibilityEvents.mouseCountFlow.collectLatest { count ->
//                mouseKeyEvents.emit(count)
//                idealTime.value = 0
//            }
        }
        viewModelScope.launch {
//            GlobalAccessibilityEvents.mouseMotionCountFlow.collectLatest { count ->
//                mouseMotionCount.emit(count)
//                idealTime.value = 0
//            }
        }
        startTime = Clock.System.now()
        storeStartTime = Clock.System.now()
            print("Clicked on tracker button")
            // Idle timer coroutine (increments idealTime every second when active)
            if (!isIdleTaskScheduled.value) {
                isIdleTaskScheduled.value = true
                idleJob = viewModelScope.launch {
                    while (isActive) {
                        delay(1000L) // wait 1 second
                        if (isIdealTimerRunning.value) {
                            idealTime.value += 1
                        }
                    }
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
                            val storeTimeDifference = currentTime.epochSeconds - storeStartTime.epochSeconds
                            if (timeDifference >= postActivityInterval) {
                                canCallApi.value = true
                            }
                            Log.d("$timeDifference and ${canCallApi.value}")
                            trackerTime.value++
                            screenShotTakenTime.value++

                            println("Current minute: ${(trackerTime.value % 3600) / 60}")
                            println("Random times: ${randomTime.value}")

                            if (trackerIndex < randomTime.value.size && trackerTime.value > randomTime.value[trackerIndex]) {
                                //takeScreenShot()
                                screenShotTakenTime.value = 0
                                trackerIndex++

                                if (trackerIndex == randomTime.value.size) {
                                    val overallStart = trackerTime.value
                                    val overallEnd = overallStart + (screenshotLimit * 60)
                                    trackerIndex = 0
                                    setRandomTimes(randomTime, overallStart, overallEnd, screenShotFrequency)
                                }
                            }
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
            mouseActivity = mouseKeyEvents.value,
            keyboardActivity = keyboradKeyEvents.value,
            totalActivity = (mouseKeyEvents.value + keyboradKeyEvents.value) % 100,
            billable = "",
            notes = "",
            uri = currentImageUri.value
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
            unTrackedTime = idealTime.value.toLong(),
            uri = currentImageUri.value
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
            uri = currentImageUri.value
        )
        storeStartTime = Clock.System.now()

        return activity
    }

    actual fun updateStartTime() {
    }

}
