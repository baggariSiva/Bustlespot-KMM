package org.softsuave.bustlespot.background

import kotlinx.coroutines.flow.StateFlow
import org.softsuave.bustlespot.utils.ActivityServiceState


actual class PostingServiceManager() {


    actual fun stopPosting() {
    }

    actual fun pausePosting() {
    }

    actual fun resumePosting() {
    }

    actual fun isServiceRunning(): Boolean {
        return false
    }

    actual fun getActiveTaskCount(): Int {
        return 0
    }

    actual val currentState: StateFlow<ActivityServiceState>?
        get() = null

    actual fun startPosting(
        postData: String,
        initialTimeMillis: Long,
        taskId: String?,
        projectId: String?,
        onStart: () -> Unit
    ) {
    }

    actual fun getCurrentElapsedTime(): Long {
        return 0L
    }

}