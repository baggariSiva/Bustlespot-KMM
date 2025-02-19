package org.softsuave.bustlespot.auth.signin.data

import kotlinx.serialization.Serializable


@Serializable
data class BaseResponse<T>(
    var status: String? = null,
    var data: T? = null,
    var message: String? = null
)