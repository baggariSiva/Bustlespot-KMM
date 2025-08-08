package org.softsuave.bustlespot.background


actual class PostingServiceManager() {

    actual fun startPosting(postData: String) {

    }

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

}