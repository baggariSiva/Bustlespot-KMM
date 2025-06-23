package org.softsuave.bustlespot.auth.signin.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.signin.data.SignInRepository
import org.softsuave.bustlespot.auth.signin.data.User
import org.softsuave.bustlespot.auth.utils.CustomTextFieldState
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.auth.utils.validateEmail
import org.softsuave.bustlespot.auth.utils.validatePassword

class LoginViewModel(
    private val repository: SignInRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _email: MutableStateFlow<CustomTextFieldState> =
        MutableStateFlow(CustomTextFieldState())
    val email: StateFlow<CustomTextFieldState> = _email

    private val _password: MutableStateFlow<CustomTextFieldState> =
        MutableStateFlow(CustomTextFieldState())
    val password: StateFlow<CustomTextFieldState> = _password

    private val _uiEvent: MutableStateFlow<UiEvent<User>?> = MutableStateFlow(null)
    val uiEvent: StateFlow<UiEvent<User>?> = _uiEvent


    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                val validationResult = validateEmail(event.email)
                _email.value = _email.value.copy(
                    value = event.email,
                    error = validationResult ?: "",
                    isValid = validationResult == null
                )
            }

            is LoginEvent.PasswordChanged -> {
                val validationResult = validatePassword(event.password)
                _password.value = _password.value.copy(
                    value = event.password,
                    error = validationResult ?: "",
                    isValid = validationResult == null
                )
            }

            is LoginEvent.SubmitLogin -> {
                viewModelScope.launch {
                    repository.signIn(email.value.value, password.value.value).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                launch {
                                    result.data.token.let { newToken ->
                                        sessionManager.accessToken = newToken
                                        sessionManager.updateAccessToken(
                                            newToken
                                        )
                                    }
                                    result.data.apply {
                                        sessionManager.userId = userId
                                        sessionManager.userFirstName = firstName
                                        sessionManager.userLastName = lastName
                                    }
                                }

                                _uiEvent.value = UiEvent.Success(result.data)
                            }

                            is Result.Error -> {
                                _uiEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                            }

                            Result.Loading -> {
                                _uiEvent.value = UiEvent.Loading
                            }
                        }
                    }
                }
            }

            LoginEvent.TextHttpClint -> viewModelScope.launch {
                repository.testRoot().collectLatest {
                    println("Result: $it")
                }
            }
        }
    }
}


// Sealed class for login events
sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object SubmitLogin : LoginEvent()
    object TextHttpClint : LoginEvent()
}

