package org.softsuave.bustlespot.background
/**
 * Common interface for managing posting service across platforms
 */

/**
 * Callback interface for posting results
 */
interface PostingResultCallback {
    fun onPostingSuccess(postId: String, message: String)
    fun onPostingError(postId: String, error: String)
    fun onPostingProgress(postId: String, progress: Int)
}



expect class PostingActivityService()

