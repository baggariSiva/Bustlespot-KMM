package org.softsuave.bustlespot.organisation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.auth.SignOutUseCase
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.data.network.models.response.Organisation
import org.softsuave.bustlespot.data.network.models.response.SignOutResponseDto
import org.softsuave.bustlespot.organisation.data.OrganisationRepository
import org.softsuave.bustlespot.tracker.data.TrackerRepository

class OrganisationViewModel(
    private val organisationRepository: OrganisationRepository,
    private val trackerRepository: TrackerRepository,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _organisationList: MutableStateFlow<List<Organisation>> =
        MutableStateFlow(emptyList())
    val organisationList: StateFlow<List<Organisation>> = _organisationList.asStateFlow()

    private val _logOutEvent: MutableStateFlow<UiEvent<SignOutResponseDto>?> =
        MutableStateFlow(null)
    val logOutEvent: StateFlow<UiEvent<SignOutResponseDto>?> = _logOutEvent.asStateFlow()
    private val _uiEvent: MutableStateFlow<UiEvent<List<Organisation>>> =
        MutableStateFlow(UiEvent.Loading)
    val uiEvent: StateFlow<UiEvent<List<Organisation>>> = _uiEvent.asStateFlow()

    private val _showLogOutDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogOutDialog: StateFlow<Boolean> = _showLogOutDialog.asStateFlow()
    private fun getAllOrganisation() {
        viewModelScope.launch {

            organisationRepository.getAllOrganisation().collect { result ->
                when (result) {
                    is Result.Error -> {
                        Log.d("${result.exception}")
                        Log.d("${result.message}")
                        _uiEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                    }

                    Result.Loading -> {
                        _uiEvent.value = UiEvent.Loading
                    }

                    is Result.Success -> {
                        _organisationList.value = result.data
                        _uiEvent.value = UiEvent.Success(result.data)
                        println("Success: ${result.data}")
                    }
                }

            }
        }
    }

    fun logOutDisMissed() {
        _showLogOutDialog.value = false
    }

    fun showLogOutDialog() {
        _showLogOutDialog.value = true
    }

    fun performLogOut() {
        viewModelScope.launch {
            signOutUseCase.invoke().collect { result ->
                when (result) {

                    is Result.Error -> {
                        _showLogOutDialog.value = false
                        _logOutEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                    }

                    is Result.Loading -> {
                        _logOutEvent.value = UiEvent.Loading
                    }

                    is Result.Success -> {
                        _showLogOutDialog.value = false
                        val data = result.data
                        _logOutEvent.value = UiEvent.Success(data)
                        println("Success: ${result.data}")
                    }
                }

            }
        }
    }

    fun checkAndPostActivities() {
        viewModelScope.launch(SupervisorJob()) {
            trackerRepository.checkLocalDbAndPostActivity()
        }
    }

    init {
        getAllOrganisation()
    }
}