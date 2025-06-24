package org.softsuave.bustlespot

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.ObservableSettings
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
}
