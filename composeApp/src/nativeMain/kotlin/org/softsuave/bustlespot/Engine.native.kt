package org.softsuave.bustlespot

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun getEngine(): HttpClientEngine {
    return Darwin.create()
}