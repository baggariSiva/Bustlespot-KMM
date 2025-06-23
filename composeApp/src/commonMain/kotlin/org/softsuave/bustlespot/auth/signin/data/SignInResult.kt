package org.softsuave.bustlespot.auth.signin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class User(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("user_first_name")
    val firstName: String = "",
    @SerialName("user_last_name")
    val lastName: String = "",
    @SerialName(" user_email")
    val email: String = "",
    @SerialName("access_token")
    val token: String = "",
    @SerialName("refresh_token")
    val refreshToken: String = "",
    @SerialName("user_profile")
    val profile: String? = null,
)
