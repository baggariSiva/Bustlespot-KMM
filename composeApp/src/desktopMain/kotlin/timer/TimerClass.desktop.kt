package org.softsuave.bustlespot.timer

import GlobalEventListener
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

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
    private val screenShotFrequency = 1
    private val screenshotLimit = 1
    private var startTimeNanos: Long = 0L
    private var idleStartTimeNanos: Long = 0L

    actual fun resetTimer() {
        isTrackerRunning.value = false
        idealTime.value = 0
        trackerTime.value = 0
    }

    actual fun stopTimer() {
        isTrackerRunning.value = false
        globalEventListener.unregisterListeners()
    }

    actual fun resumeTracker() {
        isTrackerRunning.value = true
        globalEventListener.registerListeners()
    }

    private fun setRandomTimes(randomTimes: MutableStateFlow<List<Int>>, overallStart: Int, overallEnd: Int, numberOfIntervals: Int = 10) {
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
        globalEventListener.registerListeners()
        setRandomTimes(randomTime, 0, screenshotLimit * 60, screenShotFrequency)
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
        if (!isIdleTaskScheduled.getAndSet(true)) {
            idleStartTimeNanos = System.nanoTime()
            idleTimerTask = object : TimerTask() {
                override fun run() {
                    idealTime.value = (((System.nanoTime() - idleStartTimeNanos) / 1_000_000_000).toInt())
                }
            }
            timer.scheduleAtFixedRate(idleTimerTask, 1000, 1000)
        }
        startTimeNanos = System.nanoTime()
        if (!isTaskScheduled.getAndSet(true)) {
            trackerTimerTask = object : TimerTask() {
                override fun run() {
                    if (isTrackerRunning.value) {
                        val elapsedSeconds = ((System.nanoTime() - startTimeNanos) / 1_000_000_000).toInt()
                        trackerTime.value = elapsedSeconds
                        if (elapsedSeconds % 60 == 0) {
                            screenShotTakenTime.value++
                        }
                        println("Elapsed seconds: $elapsedSeconds")
                        if (trackerIndex < randomTime.value.size && elapsedSeconds > randomTime.value[trackerIndex]) {
                            takeScreenShot()
                            trackerIndex++
                            if (trackerIndex == randomTime.value.size) {
                                val overallStart = elapsedSeconds
                                val overallEnd = overallStart + (screenshotLimit * 60)
                                trackerIndex = 0
                                setRandomTimes(randomTime, overallStart, overallEnd, screenShotFrequency)
                            }
                        }
                    }
                }
            }
            timer.scheduleAtFixedRate(trackerTimerTask, 1000, 1000)
        }
    }

    actual fun resetIdleTimer() {
        isIdealTimerRunning.value = false
        idealTime.value = 0
        idleStartTimeNanos = System.nanoTime()
    }

    actual fun stopIdleTimer() {
        isIdealTimerRunning.value = false
    }

    actual fun startIdleTimerClock() {
        idleStartTimeNanos = System.nanoTime()
        isIdealTimerRunning.value = true
    }

    fun takeScreenShot() {
        screenShot.value = org.softsuave.bustlespot.screenshot.takeScreenShot()
        screenShotTakenTime.value = 0
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


}
