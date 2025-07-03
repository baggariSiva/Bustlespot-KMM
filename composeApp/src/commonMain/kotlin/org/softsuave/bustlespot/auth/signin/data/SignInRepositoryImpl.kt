package org.softsuave.bustlespot.auth.signin.data


import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.APIEndpoints
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.data.network.models.response.ErrorResponse


class SignInRepositoryImpl(
    private val httpClient: HttpClient
) : SignInRepository {

    override suspend fun testRoot(): Flow<Result<Any>> = flow {
        try {
            val response: HttpResponse = httpClient.get(BASEURL) {
                contentType(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.OK) {
                val result = response.body<String>()
                emit(Result.Success(result))
                println("Success: $result")
            } else {
                emit(Result.Error("Failed to sign in: ${response.status}"))
                println("Failed to sign in: ${response.status}")
            }
        } catch (e: Exception) {
            emit(Result.Error("Error occurred: ${e.message}"))
            println("Error: ${e.message}")
        }
    }

    override fun signIn(email: String, password: String): Flow<Result<User>> = flow {
        try {
            emit(Result.Loading)
            val response: HttpResponse = httpClient.post("$BASEURL${APIEndpoints.SIGNIN}") {
                contentType(ContentType.Application.Json)
                header("is_tracker", "true")
                setBody(
                    SignInRequest(
                        email = email,
                        password = password
                    )
                )
            }

            if (response.status == HttpStatusCode.OK) {
                val result: BaseResponse<User> = response.body()
                emit(Result.Success(result.data ?: User()))
            } else {
                val errorResponse: BaseResponse<ErrorResponse> = response.body()
                emit(Result.Error("${errorResponse.message}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message))
        }
    }
}