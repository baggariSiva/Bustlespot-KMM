// iosMain/kotlin/org/softsuave/bustlespot/background/PostingServiceManager.ios.kt
package org.softsuave.bustlespot.background

import kotlinx.coroutines.flow.StateFlow
import org.softsuave.bustlespot.utils.ActivityServiceState

actual class PostingServiceManager {

    actual val currentState: StateFlow<ActivityServiceState>?
        get() = PostingActivityServiceIOS.currentState

    actual fun startPosting(
        postData: String,
        initialTimeMillis: Long,
        taskId: String?,
        projectId: String?,
        onStart: () -> Unit
    ) {
        PostingActivityService.startService(
            postData = postData,
            taskId = taskId,
            projectId = projectId,
            initialTimeMillis = initialTimeMillis,
            onStart = onStart
        )
    }

    actual fun stopPosting() {
        PostingActivityService.stopService()
    }

    actual fun pausePosting() {
        PostingActivityService.pauseService()
    }

    actual fun resumePosting() {
        PostingActivityService.resumeService()
    }

    actual fun isServiceRunning(): Boolean {
        return PostingActivityServiceIOS.getServiceState() == ActivityServiceState.STARTED
    }

    actual fun getActiveTaskCount(): Int {
        return PostingActivityServiceIOS.getActiveTaskCount()
    }

    actual fun getCurrentElapsedTime(): Long {
        return PostingActivityServiceIOS.getCurrentElapsedTime()
    }
}