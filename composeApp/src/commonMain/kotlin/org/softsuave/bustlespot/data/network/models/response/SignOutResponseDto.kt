package org.softsuave.bustlespot.data.network.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignOutResponseDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("session_id")
    val sessionId: String
)