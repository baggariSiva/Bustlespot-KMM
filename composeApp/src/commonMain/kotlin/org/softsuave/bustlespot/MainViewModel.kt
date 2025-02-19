package org.softsuave.bustlespot

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import org.softsuave.bustlespot.auth.signin.data.AccessTokenResponse
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.APIEndpoints
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.data.network.models.response.SignOutResponseDto

class MainViewModel(
    private val settings: ObservableSettings,
    private val httpClient: () -> HttpClient,
) : ViewModel() {


    private val _accessToken: MutableStateFlow<String> = MutableStateFlow("")
    val accessToken: StateFlow<String> get() = _accessToken

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    val isLoggedIn: Boolean get() = accessToken.value.isNotEmpty()

    fun fetchAccessToken() {
        val token = settings.getString("access_token", "")
        _accessToken.value = token
        _isLoading.value = false
    }

    fun updateAccessToken(newToken: String) {
        settings.putString("access_token", newToken)
        _accessToken.value = settings.getString("access_token", "")
    }

    // MainViewModel.kt
    suspend fun refreshAccessToken(): Result<AccessTokenResponse> {
        return try {
            println("Refreshing token...")
            val response: HttpResponse = httpClient.invoke().get("$BASEURL/auth/refresh-token") {
                bearerAuth(accessToken.value)

            }
            if (response.status == HttpStatusCode.OK) {
                val result: AccessTokenResponse = response.body()
                _accessToken.value = result.access_token
                Result.Success(result)
            } else if (
                response.status == HttpStatusCode.Gone
            ) {
                updateAccessToken("")
                println("Force Logout")
                Result.Error("Force Logout")
            } else {
                Result.Error("Failed to refresh token: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error("Exception during token refresh: ${e.message}")
        }
    }

    fun logOutSession(): Flow<Result<SignOutResponseDto>> = flow {
        try {
            emit(Result.Loading)
            val response: HttpResponse =
                httpClient.invoke().post("$BASEURL${APIEndpoints.SIGNOUT}") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status == HttpStatusCode.OK) {
                val result: SignOutResponseDto = response.body() // Deserialize the response body
                emit(Result.Success(result))
                settings.remove("access_token")
            } else {
                emit(Result.Error("Failed to sign out: ${response.status} ${response.body<Any>()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message))
        }
    }
}
