package org.softsuave.bustlespot
interface Platform {
  val platformType  : PlatFormType
}

expect fun getPlatform(): Platform


enum class PlatFormType() {
    IOS,
    ANDROID,
    DESKTOP,
    UNKNOWN
}