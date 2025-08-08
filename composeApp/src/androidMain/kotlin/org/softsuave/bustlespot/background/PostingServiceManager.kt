package org.softsuave.bustlespot.background


import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import org.softsuave.bustlespot.utils.ActivityServiceState

actual class PostingServiceManager(private val context: Context) {


    actual fun startPosting(
        postData: String, initialTimeMillis: Long,
        onStart: () -> Unit
    ) {
        PostingActivityService.startService(context, postData, initialTimeMillis,onStart)
    }

    actual fun stopPosting() {
        PostingActivityService.stopService(context)
    }

    actual fun pausePosting() {
        PostingActivityService.pauseService(context)
    }

    actual fun resumePosting() {
        PostingActivityService.resumeService(context)
    }

    actual fun isServiceRunning(): Boolean {
        val serviceInstance = PostingActivityService.getInstance()
        return serviceInstance?.getServiceState() == ActivityServiceState.STARTED
    }

    actual fun getActiveTaskCount(): Int {
        return PostingActivityService.getInstance()?.getActiveTaskCount() ?: 0
    }

    // New method to get current elapsed time
    actual fun getCurrentElapsedTime(): Long {
        return PostingActivityService.getInstance()?.getCurrentElapsedTime() ?: 0L
    }

    actual val currentState: StateFlow<ActivityServiceState>?
        get() = PostingActivityService.getInstance()?.currentState
}