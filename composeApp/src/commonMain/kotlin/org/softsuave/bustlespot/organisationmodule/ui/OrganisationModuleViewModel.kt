package org.softsuave.bustlespot.organisationmodule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.data.network.models.response.OrganisationModule
import org.softsuave.bustlespot.organisationmodule.data.OrganisationModuleRepository


class OrganisationModuleViewModel(
    private val organisationModuleRepository: OrganisationModuleRepository,
) : ViewModel() {


    private val _organisationModuleList: MutableStateFlow<List<OrganisationModule>> =
        MutableStateFlow(emptyList())
    val organisationModuleList: StateFlow<List<OrganisationModule>> =
        _organisationModuleList.asStateFlow()
    private val _uiEvent: MutableStateFlow<UiEvent<List<OrganisationModule>>> =
        MutableStateFlow(UiEvent.Loading)
    val uiEvent: StateFlow<UiEvent<List<OrganisationModule>>> = _uiEvent.asStateFlow()
    fun getOrganisationModules(
        organisationId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            organisationModuleRepository.getOrganisationModules(
                organisationId = organisationId
            ).collect { result ->
                when (result) {
                    is Result.Error -> {
                        _uiEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                    }

                    Result.Loading -> {
                        _uiEvent.value = UiEvent.Loading
                    }

                    is Result.Success -> {
                        _organisationModuleList.value = result.data
                        _uiEvent.value = UiEvent.Success(result.data)
                        println("Success: ${result.data}")
                    }
                }
            }
        }
    }
}