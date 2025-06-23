package org.softsuave.bustlespot.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.signin.data.BaseResponse
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.APIEndpoints
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.data.network.models.response.SignOutResponseDto

class SignOutUseCase(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager
) {
    operator fun invoke(): Flow<Result<SignOutResponseDto>> = flow {
        try {
            emit(Result.Loading)
            val response: HttpResponse = httpClient.post("$BASEURL${APIEndpoints.SIGNOUT}") {
                contentType(ContentType.Application.Json)
                bearerAuth(sessionManager.accessToken)
            }

            if (response.status == HttpStatusCode.OK) {
                val result: BaseResponse<SignOutResponseDto> = response.body()
                emit(Result.Success(result.data ?: SignOutResponseDto(userId = "", sessionId = "")))
                sessionManager.clearSession()
            } else {
                emit(Result.Error("Failed to sign out: ${response.status} ${response.body<Any>()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message))
        }
    }
}