package org.softsuave.bustlespot.auth.signin.data

import kotlinx.serialization.Serializable


@Serializable
data class AccessTokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
)