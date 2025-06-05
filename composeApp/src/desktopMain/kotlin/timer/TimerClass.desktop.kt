package org.softsuave.bustlespot.timer

import GlobalEventListener
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.notifications.sendLocalNotification
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.time.Duration
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
    private var timer = Timer()
    private var isTaskScheduled = AtomicBoolean(false)
    private var isIdleTaskScheduled = AtomicBoolean(false)
    private val globalEventListener: GlobalEventListener = GlobalEventListener()
    private val screenShot = MutableStateFlow<ImageBitmap?>(null)
    actual val screenShotState: StateFlow<ImageBitmap?> = screenShot
    private val randomTime: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    private var screenshotRepeatingTask: TimerTask? = null
    private var screenshotOneShotTask: TimerTask? = null


    @Volatile
    private var isPaused = false

    private var trackerTimerTask: TimerTask? = null
    private var idleTimerTask: TimerTask? = null
    private var trackerIndex = 0
    private val screenShotFrequency = 1 // n of screenshot in a slot
    private val screenshotLimit = 10 //in mints
    private var idealStartTime: Instant = Instant.DISTANT_PAST
    private val postActivityInterval: Int = 600 //in second
    private val storeActivityInterval: Int = 60 //in second

    actual fun resetTimer() {
        isTrackerRunning.value = false
        globalEventListener.unregisterListeners()
        idealTime.value = 0
        trackerTime.value = 0
    }

    actual fun stopTimer() {
        Log.d("stopTimer")
        isTrackerRunning.value = false
        idealStartTime = Clock.System.now() - customeTimeForIdleTime.value.seconds
        globalEventListener.unregisterListeners()
    }

    actual fun resumeTracker() {
        Log.d("resumeTracker")
        isTrackerRunning.value = true
        globalEventListener.registerListeners()
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
        Log.d("startTimer")
        isTrackerRunning.value = true
        isIdealTimerRunning.value = true
        globalEventListener.registerListeners()
        setRandomTimes(
            randomTime,
            overallStart = 0,
            overallEnd = screenshotLimit * 60,
            numberOfIntervals = screenShotFrequency
        )
        trackerIndex = 0
        viewModelScope.launch {
            globalEventListener.fKeyCount.collectLatest { count ->
                keyboradKeyEvents.emit(count)
                idealTime.value = 0
            }
        }
        viewModelScope.launch {
            globalEventListener.fMouseCount.collectLatest { count ->
                mouseKeyEvents.emit(count)
                idealTime.value = 0
            }
        }
        viewModelScope.launch {
            globalEventListener.fMouseMotionCount.collectLatest { count ->
                mouseMotionCount.emit(count)
                idealTime.value = 0
            }
        }
        startTime = Clock.System.now()
        storeStartTime = Clock.System.now()
        if (!isIdleTaskScheduled.getAndSet(true)) {
            idleTimerTask = object : TimerTask() {
                override fun run() {
                    if (isIdealTimerRunning.value) {
                        idealTime.value += 1
                    }
                }
            }
            timer.scheduleAtFixedRate(idleTimerTask, 1000, 1000)
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
                        }
                        if (storeTimeDifference >= storeActivityInterval) {
                            canStoreApiCall.value = true
                        }
                        Log.d("$timeDifference and ${canCallApi.value}")
                        trackerTime.value++
                        // need to add initial ideal time
                        screenShotTakenTime.value++
                        println("Current minute: ${(trackerTime.value % 3600) / 60}")
                        println("Random times: ${randomTime.value}")
                        if (trackerIndex < randomTime.value.size && trackerTime.value > randomTime.value[trackerIndex]) {
                            takeScreenShot()
                            screenShotTakenTime.value = 0
                            trackerIndex++
                            if (trackerIndex == randomTime.value.size) {
                                val overallStart = trackerTime.value
                                val overallEnd = overallStart + (screenshotLimit * 60)
                                trackerIndex = 0
                                setRandomTimes(
                                    randomTime,
                                    overallStart,
                                    overallEnd,
                                    screenShotFrequency
                                )
                            }
                        }
                    }
                }
            }
            timer.scheduleAtFixedRate(trackerTimerTask, 1000, 1000)
        }
    }

    actual fun resetIdleTimer() {
        idealTime.value = 0
    }

    actual fun stopIdleTimer() {
        isIdealTimerRunning.value = false
        globalEventListener.unregisterListeners()
    }

    actual fun startIdleTimerClock() {
        isIdealTimerRunning.value = true
    }

    fun takeScreenShot() {
        screenShot.value = org.softsuave.bustlespot.screenshot.takeScreenShot() ?: return
        val bufferedImage: BufferedImage = screenShot.value?.toAwtImage() ?: return

        val file = File(System.getProperty("java.io.tmpdir"), "sampleFile.png")

        ByteArrayOutputStream().use { byteArrayOutputStream ->
            ImageIO.write(bufferedImage, "png", file)
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream)
//            currentImageUri.value =
//                Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
        }
        sendLocalNotification(
            "Bustle-spot Reminder",
            "Captured screenshot",
            imageFile = file.absolutePath
        )
    }

    actual fun startScreenshotTask() {
        if (screenshotRepeatingTask == null) {
            screenshotRepeatingTask = object : TimerTask() {
                override fun run() {
                    if (!isPaused) {
                        val randomDelay = Random.nextLong(0, 60 * 1000)
                        screenshotOneShotTask?.cancel()
                        screenshotOneShotTask = object : TimerTask() {
                            override fun run() {
                                takeScreenShot()
                            }
                        }
                        timer.scheduleAtFixedRate(screenshotOneShotTask, 0, randomDelay)
                    }
                }
            }
            timer.scheduleAtFixedRate(screenshotRepeatingTask, 0, 60 * 1000)
        }
    }

    actual fun pauseScreenshotTask() {
        isPaused = true
    }

    actual fun resumeScreenshotTask() {
        isPaused = false
    }

    actual fun stopScreenshotTask() {
        screenshotRepeatingTask?.cancel()
        screenshotRepeatingTask = null
        screenshotOneShotTask?.cancel()
        screenshotOneShotTask = null
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

    actual fun updateStartTime() {
        startTime = Clock.System.now()
    }

    actual var startTime: Instant = Instant.DISTANT_FUTURE
    actual var storeStartTime: Instant = Instant.DISTANT_FUTURE

    actual fun getIdleTime(): Int {
        val time =
            (startTime.epochSeconds.seconds.inWholeSeconds - globalEventListener.lastClickTime.epochSeconds.seconds.inWholeSeconds).toInt()
        println("untracked time idle$time")
        return if (time < customeTimeForIdleTime.value.seconds.inWholeSeconds && time > 0) time else 0
    }

    actual fun getActivityData(): ActivityData {
//        base64Converter()
        val endTime = getEndTime()
//        val endTime = globalEventListener.lastClickTime
        val intervalInSeconds =
            endTime.epochSeconds.seconds.inWholeSeconds - startTime.epochSeconds.seconds.inWholeSeconds
        println(intervalInSeconds)
        println((mouseKeyEvents.value + keyboradKeyEvents.value) / intervalInSeconds.toFloat())
        val activity = ActivityData(
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            mouseActivity = mouseKeyEvents.value,
            keyboardActivity = keyboradKeyEvents.value,
            totalActivity = getActivityPercentage(),
            billable = "",
            notes = "",
            uri =base64Converter()
        )
        startTime = endTime
        globalEventListener.resetClickCount()
        canCallApi.value = false
        return activity
    }

    fun getActivityPercentage(): Int {
        val endTime = getEndTime()
        val intervalInSeconds =
            endTime.epochSeconds.seconds.inWholeSeconds - startTime.epochSeconds.seconds.inWholeSeconds
        if (intervalInSeconds.toInt() == 0) return 0
        val activityPercentage =
            (((mouseKeyEvents.value + keyboradKeyEvents.value) / intervalInSeconds.toFloat()) * 100).toInt()

        val dragEvents: Int = when (activityPercentage) {
            in 0..10 -> (mouseMotionCount.value * 0.08f).toInt()
            in 10..20 -> (mouseMotionCount.value * 0.07f).toInt()
            in 20..30 -> (mouseMotionCount.value * 0.06f).toInt()
            in 30..40 -> (mouseMotionCount.value * 0.05f).toInt()
            in 40..50 -> (mouseMotionCount.value * 0.04f).toInt()
            else -> 0
        }

        val percentage =
            (((mouseKeyEvents.value + keyboradKeyEvents.value + dragEvents) / intervalInSeconds.toFloat()) * 100).toInt()


        return if (percentage > 100) 100 else percentage
    }

    actual fun getStoreActivityData(): ActivityData {
//        base64Converter()
        val endTime = getEndTime()
        val intervalInSeconds =
            endTime.epochSeconds.seconds.inWholeSeconds - storeStartTime.epochSeconds.seconds.inWholeSeconds
        println(intervalInSeconds)
        println((mouseKeyEvents.value + keyboradKeyEvents.value) / intervalInSeconds.toFloat())
        val activity = ActivityData(
            startTime = storeStartTime.toString(),
            endTime = endTime.toString(),
            mouseActivity = mouseKeyEvents.value,
            keyboardActivity = keyboradKeyEvents.value,
            totalActivity = getActivityPercentage(),
            billable = "",
            notes = "",
            uri = base64Converter()
        )
        storeStartTime = endTime
        return activity
    }

    private fun getEndTime(): Instant {
        // check and send the correct end time
//        if (idealTime.value > customeTimeForIdleTime.value) {
//            return (Clock.System.now() - customeTimeForIdleTime.value.seconds)
//        }
        return Clock.System.now()
    }

    actual fun getUntrackedActivityData(): ActivityData {
//        base64Converter()
        val activity = ActivityData(
            startTime = idealStartTime.toString(),
            endTime = Clock.System.now().toString(),
            mouseActivity = 0,
            keyboardActivity = 0,
            totalActivity = 0,
            billable = "",
            notes = "",
            unTrackedTime = idealTime.value.toLong(),
            uri = null //no photo for untracked activity
        )
        startTime = Clock.System.now()
        globalEventListener.resetClickCount()
        return activity
    }

    private fun base64Converter():String {
//        screenShot.value?.toString()?.let { Log.d("this is great $it") }
        return screenShot.value?.let {
                val byteArrayOutputStream = ByteArrayOutputStream()
                ImageIO.write(it.toAwtImage(), "png", byteArrayOutputStream)
                val bytes = byteArrayOutputStream.toByteArray()
                Log.d("$bytes y")
                Base64.getEncoder().encodeToString(bytes)
            }.toString()
    }


    actual var canCallApi: MutableStateFlow<Boolean> = MutableStateFlow(false)
    actual var canStoreApiCall: MutableStateFlow<Boolean> = MutableStateFlow(false)
}
