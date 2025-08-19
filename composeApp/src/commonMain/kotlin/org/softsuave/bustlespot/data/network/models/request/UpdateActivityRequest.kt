package org.softsuave.bustlespot.data.network.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateActivityRequest(
    @SerialName("time")
    val time: Int
)