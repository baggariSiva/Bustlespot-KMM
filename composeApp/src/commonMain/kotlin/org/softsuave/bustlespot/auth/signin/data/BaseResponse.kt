package org.softsuave.bustlespot.auth.signin.data

import kotlinx.serialization.Serializable


@Serializable
data class BaseResponse<T>(
    var status: Boolean = false,
    var data: T? = null,
    var message: String? = null
)