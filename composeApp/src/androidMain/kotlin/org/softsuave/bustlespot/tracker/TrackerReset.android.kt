package org.softsuave.bustlespot.tracker

import android.content.Context
import androidx.work.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

actual fun scheduleWork(performTask: () -> Unit) {

    TaskExecutor.performTask = performTask
    val request = OneTimeWorkRequestBuilder<DailyWorker>()
        .setInitialDelay(getMillisUntilNextRun(), TimeUnit.MILLISECONDS)
        .build()

//    ComponentActivityReference.getActivity()?.baseContext?.applicationContext?.let { context ->
//        WorkManager.getInstance(
//            context = context
//        )
//    }?.enqueueUniqueWork(
//        "DailyTask",
//        ExistingWorkPolicy.REPLACE,
//        request
//    )
}

class DailyWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        TaskExecutor.performTask?.invoke()
        return Result.success()
    }
}

object TaskExecutor {
    var performTask: (() -> Unit)? = null
}


fun getMillisUntilNextRun(): Long {
    val now = LocalDateTime.now()
    val nextRun = now.withHour(3).withMinute(0).withSecond(0).withNano(0)

    val nextRunMillis = if (now.isAfter(nextRun)) {
        nextRun.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } else {
        nextRun.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    return nextRunMillis - System.currentTimeMillis()
}
