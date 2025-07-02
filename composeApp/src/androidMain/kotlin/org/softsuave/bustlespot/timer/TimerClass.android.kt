package org.softsuave.bustlespot.timer

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.MainActivity
import org.softsuave.bustlespot.accessability.GlobalAccessibilityEvents
import org.softsuave.bustlespot.notifications.sendLocalNotification
import org.softsuave.bustlespot.screenshot.ComponentActivityReference
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import org.softsuave.bustlespot.ui.MediaProjectionService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
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
    private val screenShot = MutableStateFlow<ImageBitmap?>(null)
    actual val screenShotState: StateFlow<ImageBitmap?> = screenShot
    private val randomTime: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    private var screenshotRepeatingTask: TimerTask? = null
    private var screenshotOneShotTask: TimerTask? = null
    private var isObservingEvents: Boolean = false

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

    actual fun resetTimer() {
        isTrackerRunning.value = false
        GlobalAccessibilityEvents.isListening.value = false
        //    globalEventListener.unregisterListeners()
        idealTime.value = 0
        Log.d("idle time rested")
        trackerTime.value = 0
    }

    actual fun stopTimer() {
        Log.d("stopTimer")
        isTrackerRunning.value = false
        idealStartTime = Clock.System.now()
        ComponentActivityReference.getActivity()?.apply {
            val serviceIntent = Intent(this, MediaProjectionService::class.java)
            stopService(serviceIntent)
        }
        GlobalAccessibilityEvents.isListening.value = false
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
        GlobalAccessibilityEvents.isListening.value = true
        //    globalEventListener.registerListeners()

        val serviceIntent = Intent(
            ComponentActivityReference.getActivity(),
            MediaProjectionService::class.java
        ).apply {
            putExtra("resultCode", MainActivity.ProjectionData.resultCode) // Pass resultCode
            putExtra("data", MainActivity.ProjectionData.data) // Pass data

        }
        ComponentActivityReference.getActivity()?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startForegroundService(serviceIntent) // Use startForegroundService for Android O+
            } else {
                it.startService(serviceIntent) // Use startService for older versions
            }
        }
        setRandomTimes(
            randomTime,
            overallStart = 0,
            overallEnd = screenshotLimit * 60,
            numberOfIntervals = screenShotFrequency
        )
        trackerIndex = 0
        CoroutineScope(Dispatchers.IO).launch {
            GlobalAccessibilityEvents.keyCountFlow.collectLatest { count ->
                if (isTrackerRunning.value) {
                    keyboradKeyEvents.emit(count)
                    idealTime.value = 0
                    Log.d("idle time rested1")
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            GlobalAccessibilityEvents.mouseCountFlow.collectLatest { count ->
                if (isTrackerRunning.value) {
                    mouseKeyEvents.emit(count)
                    idealTime.value = 0
                    Log.d("idle time rested2")
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            GlobalAccessibilityEvents.mouseMotionCountFlow.collectLatest { count ->
                if (isTrackerRunning.value) {
                    mouseMotionCount.emit(count)
                    idealTime.value = 0
                    Log.d("idle time rested3")
                }
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
            timer.schedule(idleTimerTask, 1000, 1000)
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
            timer.schedule(trackerTimerTask, 1000, 1000)
        }
    }

    actual fun resetIdleTimer() {
        idealTime.value = 0
        Log.d("idle time rested")
    }

    actual fun stopIdleTimer() {
        isIdealTimerRunning.value = false
        GlobalAccessibilityEvents.isListening.value = false
        // globalEventListener.unregisterListeners()
    }

    actual fun startIdleTimerClock() {
        isIdealTimerRunning.value = true
    }

    actual fun getIdleTime(): Int {
        val time = (startTime.epochSeconds.seconds.inWholeSeconds - GlobalAccessibilityEvents.lastClickTime.epochSeconds.seconds.inWholeSeconds).toInt()
        return if (time < customeTimeForIdleTime.value.seconds.inWholeSeconds && time > 0) time else 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun takeScreenShot() {
        screenShot.value = org.softsuave.bustlespot.screenshot.takeScreenShot()
        screenShot.value?.let { saveImageAndConvertToBase64(it) }?.let {
            currentImageUri.value = it
        }
        sendLocalNotification(
            "Bustle-spot Remainder",
            "Captured screen-shot", null
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
                        timer.schedule(screenshotOneShotTask, randomDelay)
                    }
                }
            }
            timer.schedule(screenshotRepeatingTask, 0, 60 * 1000)
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

    actual var startTime: Instant = Instant.DISTANT_FUTURE


    actual fun getActivityData(): ActivityData {
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
            uri = currentImageUri.value
        )
        storeStartTime = Clock.System.now()
        return activity
    }

    actual fun updateStartTime() {
        startTime = Clock.System.now()
    }

}
