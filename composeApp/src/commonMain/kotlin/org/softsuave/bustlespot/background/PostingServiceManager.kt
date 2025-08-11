package org.softsuave.bustlespot.background

import kotlinx.coroutines.flow.StateFlow
import org.softsuave.bustlespot.utils.ActivityServiceState

/**
 * Common interface for managing posting service across platforms
 */
expect class PostingServiceManager {

    val currentState: StateFlow<ActivityServiceState>?

    fun startPosting(
        postData: String,
        initialTimeMillis: Long = 0L,
        taskId: String? = null,
        projectId: String? = null,
        onStart: () -> Unit = {},
    )

    fun stopPosting()

    fun pausePosting()

    fun resumePosting()

    fun isServiceRunning(): Boolean

    fun getActiveTaskCount(): Int

    fun getCurrentElapsedTime(): Long
}