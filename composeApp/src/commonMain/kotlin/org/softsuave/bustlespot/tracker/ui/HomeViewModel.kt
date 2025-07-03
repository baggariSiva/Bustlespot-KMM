package org.softsuave.bustlespot.tracker.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.PlatFormType
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.auth.utils.timeStringToSeconds
import org.softsuave.bustlespot.data.network.models.request.UpdateActivityRequest
import org.softsuave.bustlespot.data.network.models.response.OrganisationModule
import org.softsuave.bustlespot.data.network.models.response.Project
import org.softsuave.bustlespot.data.network.models.response.TaskData
import org.softsuave.bustlespot.getPlatform
import org.softsuave.bustlespot.locationmodule.LocationViewModel
import org.softsuave.bustlespot.network.NetworkMonitor
import org.softsuave.bustlespot.timer.TrackerModule
import org.softsuave.bustlespot.tracker.data.TrackerRepository
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import kotlin.math.roundToInt

class HomeViewModel(
    private val sessionManager: SessionManager,
    private val trackerRepository: TrackerRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private var _platFormType: MutableStateFlow<PlatFormType> = MutableStateFlow(getPlatform().platformType)
    val platFormType = _platFormType.asStateFlow()

    private val trackerModule = TrackerModule(viewModelScope)
    val trackerTime: StateFlow<Int> = trackerModule.trackerTime
    val isTrackerRunning: StateFlow<Boolean> = trackerModule.isTrackerRunning
    val idealTime: StateFlow<Int> = trackerModule.idealTime
    var screenShotTakenTime: StateFlow<Int> = trackerModule.screenShotTakenTime
    val customeTimeForIdleTime: StateFlow<Int> = trackerModule.customeTimeForIdleTime
    val screenShotState: StateFlow<ImageBitmap?> = trackerModule.screenShotState
    var canCallApi: MutableStateFlow<Boolean> = trackerModule.canCallApi
    var canStoreApiCall: MutableStateFlow<Boolean> = trackerModule.canStoreApiCall
    var lastSyncTime: MutableStateFlow<Long> = MutableStateFlow(0)


    fun startTrackerTimer() = trackerModule.startTimer()



    private fun constructPostActivityRequest(
        activityDataOfModule: ActivityData
    ): ActivityData {
        activityDataOfModule.apply {
            this.taskId = _selectedTask.value?.taskId
            this.projectId = _selectedProject.value?.projectId
            if (_isOnSiteSelected.value) {
                this.uri = null
                this.latitude = coordinateInfo.value.latitude
                this.longitude = coordinateInfo.value.longitude
            }
        }
        return activityDataOfModule
    }

    fun startPostingActivity(
        showLoading: Boolean = false,
        doActionOnSuccess: () -> Unit = {}
    ) {
        try {
            val request = constructPostActivityRequest(
                activityDataOfModule = trackerModule.getActivityData()
            )
            Log.d("$request----reguest")
            postUserActivity(request, showLoading, doActionOnSuccess)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun storePostActivity(
    ) {
        try {
            val request = constructPostActivityRequest(
                activityDataOfModule = trackerModule.getStoreActivityData()
            )
//            canStoreApiCall.value = !trackerRepository.storePostUserActivity(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startPostingUntrackedActivity() {
        try {
            val request = constructPostActivityRequest(
                activityDataOfModule = trackerModule.getUntrackedActivityData()
            )
            Log.d("$request----reguest")
            postUserActivity(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun postUpdateActivity(
        organisationId: Int
    ) {
        try {
            val request = UpdateActivityRequest(
                organisationId,
                trackerModule.getIdleTime()
            )
            Log.d("$request----reguest")
            postUpdateActivity(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopTrackerTimer() = trackerModule.stopTimer()
    fun resetTrackerTimer() = trackerModule.resetTimer()
    fun resumeTrackerTimer() = trackerModule.resumeTracker()
    fun updateStartTime() = trackerModule.updateStartTime()


    fun resetIdleTimer() = trackerModule.resetIdleTimer()
    fun updateTrackerTimer() = trackerModule.updateTrackerTimer()


    fun stopIdleTimer() = trackerModule.stopIdleTimer()

    val locationViewModel = LocationViewModel()

    val locationInfo = locationViewModel.locationInfo
    val geoFenceInfo = locationViewModel.geoFenceInfo
    val coordinateInfo = locationViewModel.coordinateInfo


    private val _mainTaskList =
        MutableStateFlow<List<TaskData>>(emptyList())
    private val _mainProjectList =
        MutableStateFlow<List<Project>>(emptyList())

    private val _uiEvent =
        MutableStateFlow<UiEvent<TrackerScreenData>>(UiEvent.Loading)
    val uiEvent: StateFlow<UiEvent<TrackerScreenData>> get() = _uiEvent

    private val _dialogEvent =
        MutableStateFlow(false)
    val dialogEvent: StateFlow<Boolean> get() = _dialogEvent

    private val trackerScreenData = TrackerScreenData(true)

    private val _isOnSiteSelected = MutableStateFlow<Boolean>(false)
    val isOnSiteSelected: StateFlow<Boolean> = _isOnSiteSelected.asStateFlow()


    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskData?>(null)
    val selectedTask: StateFlow<TaskData?> = _selectedTask.asStateFlow()

    private val _selectedModule = MutableStateFlow<OrganisationModule?>(null)
    val selectedModule: StateFlow<OrganisationModule?> = _selectedModule.asStateFlow()

    private val _moduleDropDownState = MutableStateFlow(DropDownState<OrganisationModule>())
    val moduleDropDownState: StateFlow<DropDownState<OrganisationModule>> =
        _moduleDropDownState.asStateFlow()

    private val _projectDropDownState = MutableStateFlow(DropDownState<Project>())
    val projectDropDownState: StateFlow<DropDownState<Project>> =
        _projectDropDownState.asStateFlow()

    private val _taskDropDownState = MutableStateFlow(DropDownState<TaskData>())
    val taskDropDownState: StateFlow<DropDownState<TaskData>> = _taskDropDownState.asStateFlow()

    private val _trackerDialogState: MutableStateFlow<TrackerDialogState> =
        MutableStateFlow(TrackerDialogState())
    val trackerDialogState: StateFlow<TrackerDialogState> = _trackerDialogState.asStateFlow()

    private val _totalIdleTime: MutableStateFlow<Int> = MutableStateFlow(0)
    val totalIdleTime: StateFlow<Int> = _totalIdleTime.asStateFlow()

    // actual is 7200ms -- 120 mins
    private val idealTimeThreshold: Int = 7200

    private fun selectModuleAsPerPlatform(
        platformType: PlatFormType
    ) {
        when (platformType) {
            PlatFormType.IOS, PlatFormType.ANDROID -> {
                val selected = _moduleDropDownState.value.dropDownList.firstOrNull {
                    it.moduleName.contains("onsite", ignoreCase = true)
                }
                _selectedModule.value = selected
                setModuleAndGetProTas(
                    selected ?: _moduleDropDownState.value.dropDownList.firstOrNull() ?: return
                )
            }

            PlatFormType.DESKTOP -> {
                val selected = _moduleDropDownState.value.dropDownList.firstOrNull {
                    it.moduleName.contains("it", ignoreCase = true)
                }
                _selectedModule.value = selected
                setModuleAndGetProTas(
                    selected ?: _moduleDropDownState.value.dropDownList.firstOrNull() ?: return
                )
            }

            PlatFormType.UNKNOWN -> {
                // TODO: handle unknown
            }
        }

    }

    private fun setModuleAndGetProTas(selectedModule: OrganisationModule) {
        _moduleDropDownState.value = _moduleDropDownState.value.copy(
            inputText = selectedModule.moduleName.toString(),
            errorMessage = ""
        )
        _projectDropDownState.value = _projectDropDownState.value.copy(
            inputText = ""

        )
        _taskDropDownState.value = _taskDropDownState.value.copy(
            inputText = ""
        )
        _selectedProject.value = null
        _selectedTask.value = null
        resetTrackerTimer()
        getAllProjects(selectedModule.moduleId.toString())
    }

    fun getAllModules(organisationId: String) {
        viewModelScope.launch {
            trackerRepository.getAllModules(organisationId).collect { result ->
                when (result) {
                    is Result.Error -> {
                        _uiEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                        _moduleDropDownState.value = _moduleDropDownState.value.copy(
                            errorMessage = result.message ?: "Failed to fetch projects"
                        )
                    }

                    Result.Loading -> {
                        _uiEvent.value = UiEvent.Loading
                    }

                    is Result.Success -> {
                        _moduleDropDownState.update {
                            DropDownState(
                                dropDownList = result.data.toList<OrganisationModule>(),
                                errorMessage = if (result.data.isEmpty()) "No modules to select" else ""
                            )
                        }
                        selectModuleAsPerPlatform(_platFormType.value)
                        _uiEvent.update { UiEvent.Success(trackerScreenData) }
                    }
                }
            }

        }
    }

    fun getAllProjects(moduleId: String) {
        viewModelScope.launch {
            trackerRepository.getAllProjects(moduleId).collect { result ->
                when (result) {
                    is Result.Error -> {
                        _uiEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                        _projectDropDownState.value = _projectDropDownState.value.copy(
                            errorMessage = result.message ?: "Failed to fetch projects"
                        )
                    }

                    is Result.Loading -> {
                        _uiEvent.value = UiEvent.Loading
                    }

                    is Result.Success -> {
                        _mainProjectList.value = result.data
                        _projectDropDownState.value = _projectDropDownState.value.copy(
                            dropDownList = result.data,
                            errorMessage = if (result.data.isEmpty()) "No projects to select" else ""
                        )
                        trackerScreenData.isSuccess
                        _uiEvent.update { UiEvent.Success(trackerScreenData) }
                        fetchAllTasksForProjects(
                            projects = result.data
                        )
                    }
                }
            }
        }
    }

    private fun fetchAllTasksForProjects(projects: List<Project>) {
        viewModelScope.launch {
            projects.forEach { project ->
                trackerRepository.getAllTask(
                    project.projectId.toString()
                ).collect { result ->
                    when (result) {
                        is Result.Error -> {
                            Log.d("Error at projects ${result.message} project :${project}")
                            _taskDropDownState.value = _taskDropDownState.value.copy(
                                errorMessage = result.message ?: "Failed to fetch tasks"
                            )
                        }

                        is Result.Loading -> {
                            Log.d("Loading at projects")
                        }

                        is Result.Success -> {
                            val taskList = result.data.taskDetails
                            _mainTaskList.value = _mainTaskList.value.plus(taskList)
                        }
                    }
                }
            }
        }
    }

    private fun postUpdateActivity(updateActivityRequest: UpdateActivityRequest) {
        viewModelScope.launch {
            trackerRepository.updateActivity(updateActivityRequest).collect { result ->
                when (result) {
                    is Result.Error -> {
                        Log.d("Error at updating activity ${result.message}")
                    }

                    is Result.Loading -> {

                    }

                    is Result.Success -> {
                        Log.d("Success at updating activity")
                    }
                }
            }
        }
    }

    private fun postUserActivity(
        postActivityRequest: ActivityData, showLoading: Boolean = false,
        doActionOnSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            trackerRepository.postUserActivity(
                postActivityRequest
            ).collect { result ->
                when (result) {
                    is Result.Error -> {
//                        _taskDropDownState.value = _taskDropDownState.value.copy(
//                            errorMessage = result.message ?: "Failed to post activity"
//                        )
                        _dialogEvent.update { false }
                    }

                    is Result.Loading -> {
                        Log.d("posting activity")
                        _dialogEvent.update { showLoading }
                    }

                    is Result.Success -> {
                        val taskList = result.data
                        lastSyncTime.value = Clock.System.now().epochSeconds
                        _dialogEvent.update { false }
                        doActionOnSuccess()
                        Log.d(taskList.toString())
                    }
                }
            }
        }
    }

    fun checkAndPostActivities() {
        viewModelScope.launch(SupervisorJob()) {
//            trackerRepository.checkLocalDbAndPostActivity()
            networkMonitor.isConnected.collect { isNetworkAvailable ->
                if (isNetworkAvailable) {
                    trackerRepository.checkLocalDbAndPostFailedActivity()
                }
            }
        }
    }

    fun handleDropDownEvents(dropDownEvents: DropDownEvents) {
        when (dropDownEvents) {
            is DropDownEvents.OnProjectSearch -> {
                _projectDropDownState.value = _projectDropDownState.value.copy(
                    inputText = dropDownEvents.inputText
                )
                if (dropDownEvents.inputText.isEmpty() && !isTrackerRunning.value) {
                    _selectedProject.value = null
                    _taskDropDownState.value =
                        _taskDropDownState.value.copy(dropDownList = emptyList(), inputText = "")
                }
                if (_selectedModule.value == null) {
                    _moduleDropDownState.value = _moduleDropDownState.value.copy(
                        errorMessage = "Please select the a module"
                    )
                }
            }

            is DropDownEvents.OnProjectSelection -> {
                _selectedProject.value = dropDownEvents.selectedProject
                _projectDropDownState.value = _projectDropDownState.value.copy(
                    inputText = dropDownEvents.selectedProject.name,
                    errorMessage = ""
                )
                _selectedTask.value = null
                val taskList =
                    _mainTaskList.value.filter { task -> task.projectId == _selectedProject.value?.projectId }
                _taskDropDownState.value = _taskDropDownState.value.copy(
                    inputText = "",
                    dropDownList = taskList,
                    errorMessage = if (taskList.isEmpty()) "No tasks to select" else ""
                )
            }

            is DropDownEvents.OnTaskSearch -> {
                if (dropDownEvents.inputText.isNotEmpty()) {
                    _taskDropDownState.value = _taskDropDownState.value.copy(
                        inputText = dropDownEvents.inputText
                    )
                }
                if (dropDownEvents.inputText.isEmpty() && !isTrackerRunning.value) {
                    _selectedTask.value = null
                    _taskDropDownState.value = _taskDropDownState.value.copy(
                        inputText = ""
                    )
                }
                if (_selectedProject.value == null) {
                    _projectDropDownState.value = _projectDropDownState.value.copy(
                        errorMessage = "Please select the a project"
                    )
                }
            }

            is DropDownEvents.OnTaskSelection -> {
                _selectedTask.value = dropDownEvents.selectedTask
                _taskDropDownState.value = _taskDropDownState.value.copy(
                    inputText = dropDownEvents.selectedTask.name,
                    errorMessage = ""
                )
                trackerModule.setTrackerTime(
                    dropDownEvents.selectedTask.time.toInt(),
                    dropDownEvents.selectedTask.unTrackedTime.toInt()
                )
                trackerModule.setLastScreenShotTime(
                    dropDownEvents.selectedTask.lastScreenShotTime?.timeStringToSeconds() ?: 0
                )
                _totalIdleTime.value = dropDownEvents.selectedTask.unTrackedTime.roundToInt()
            }

            is DropDownEvents.OnProjectDropDownClick -> {
                if (_selectedModule.value == null) {
                    _moduleDropDownState.value = _moduleDropDownState.value.copy(
                        errorMessage = "Please select the a module"
                    )
                } else {
                    _moduleDropDownState.value =
                        _moduleDropDownState.value.copy(errorMessage = "")
                }
            }

            is DropDownEvents.OnTaskDropDownClick -> {
                if (_selectedProject.value == null) {
                    _projectDropDownState.value = _projectDropDownState.value.copy(
                        errorMessage = "Please select the a project"
                    )
                } else {
                    _projectDropDownState.value =
                        _projectDropDownState.value.copy(errorMessage = "")
                }
            }

            is DropDownEvents.OnProjectDismiss -> {
                if (selectedProject.value != null) {
                    _projectDropDownState.value = _projectDropDownState.value.copy(
                        inputText = selectedProject.value?.name ?: "",
                    )
                }
            }

            is DropDownEvents.OnTaskDismiss -> {
                if (selectedTask.value != null) {
                    _taskDropDownState.value = _taskDropDownState.value.copy(
                        inputText = selectedTask.value?.name ?: "",
                    )
                }
            }

            is DropDownEvents.OnModuleDismiss -> {
                if (_selectedModule.value != null) {
                    _moduleDropDownState.value = _moduleDropDownState.value.copy(
                        inputText = selectedModule.value?.moduleName ?: "",
                    )
                }
            }

            is DropDownEvents.OnModuleSearch -> {
                _moduleDropDownState.value = _moduleDropDownState.value.copy(
                    inputText = dropDownEvents.inputText
                )
                if (dropDownEvents.inputText.isEmpty() && !isTrackerRunning.value) {
                    _selectedModule.value = null
                    _projectDropDownState.value =
                        _projectDropDownState.value.copy(dropDownList = emptyList(), inputText = "")
                }
            }

            is DropDownEvents.OnModuleSelection -> {
                if (_selectedModule.value == dropDownEvents.selectedModule) {
                    _moduleDropDownState.value = _moduleDropDownState.value.copy(
                        inputText = dropDownEvents.selectedModule.moduleName,
                        errorMessage = ""
                    )
                } else {
                    if (_platFormType.value == PlatFormType.DESKTOP) {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = true,
                            title = "Alert",
                            text = "Oops! '${dropDownEvents.selectedModule.moduleName}' isn’t available on desktop. Please try it on the mobile version.",
                            confirmButtonText = "Ok",
                            showDismissButton = false,
                            onConfirm = {
                                _trackerDialogState.value = _trackerDialogState.value.copy(
                                    isDialogShown = false,
                                )
                            }
                        )
                    }
                    if (_platFormType.value == PlatFormType.ANDROID || _platFormType.value == PlatFormType.IOS) {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = true,
                            title = "Alert",
                            text = "Oops! '${dropDownEvents.selectedModule.moduleName}' isn’t available on ${_platFormType.value.name.lowercase()} devices. Please try it on the desktop version.",
                            confirmButtonText = "Ok",
                            showDismissButton = false,
                            onConfirm = {
                                _trackerDialogState.value = _trackerDialogState.value.copy(
                                    isDialogShown = false,
                                )
                            }
                        )
                    }
                }
            }

            is DropDownEvents.OnModuleDropDownClick -> {
                print("Module dropdown clicked")
            }
        }
    }


    fun handleTrackerDialogEvents(
        trackerDialogEvents: TrackerDialogEvents,
        handleNavAction: () -> Unit = {}
    ) {
        when (trackerDialogEvents) {
            is TrackerDialogEvents.ShowExitDialog -> {
                _trackerDialogState.value = _trackerDialogState.value.copy(
                    isDialogShown = true,
                    title = "Exit",
                    text = "Are you sure you want to exit?",
                    confirmButtonText = "Yes",
                    dismissButtonText = "No",
                    onConfirm = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                        stopTrackerTimer()
                        handleNavAction()
                    },
                    onDismiss = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                    }
                )
            }

            is TrackerDialogEvents.ShowIdleTimeDialog -> {
                _trackerDialogState.value = _trackerDialogState.value.copy(
                    isDialogShown = true,
                    title = "IdleTime",
                    text = "You are idle for %s. Do you want to add idle time to the session?",
                    confirmButtonText = "Okay",
                    dismissButtonText = "Cancel",
                    onConfirm = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                        if (idealTime.value < idealTimeThreshold)
                            _totalIdleTime.value += idealTime.value
                        handleNavAction()
                        resetIdleTimer()
                        resumeTrackerTimer()
                    },
                    onDismiss = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                        resetIdleTimer()
                        resumeTrackerTimer()
                        updateStartTime()
                    }
                )
            }

            is TrackerDialogEvents.ShowProjectChangeDialog -> {
                _trackerDialogState.value = _trackerDialogState.value.copy(
                    isDialogShown = true,
                    title = "Alert",
                    text = "Are you sure you want to stop tracker and change project?",
                    confirmButtonText = "Yes",
                    dismissButtonText = "No",
                    onConfirm = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                        updateSelectedTaskTime(trackerTime.value, idealTime.value)
                        resetTrackerTimer()
                        resetIdleTimer()
                        handleNavAction()
                        handleDropDownEvents(DropDownEvents.OnProjectSelection(trackerDialogEvents.selectedProject))
                    },
                    onDismiss = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                    }
                )
            }

            is TrackerDialogEvents.ShowTaskChangeDialog -> {
                _trackerDialogState.value = _trackerDialogState.value.copy(
                    isDialogShown = true,
                    title = "Alert",
                    text = "Are you sure you want to stop tracker and change Task?",
                    confirmButtonText = "Yes",
                    dismissButtonText = "No",
                    onConfirm = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                        updateSelectedTaskTime(trackerTime.value, idealTime.value)
                        resetTrackerTimer()
                        resetIdleTimer()
                        handleNavAction()
                        handleDropDownEvents(DropDownEvents.OnTaskSelection(trackerDialogEvents.selectedTask))
                    },
                    onDismiss = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                    }
                )
            }

            is TrackerDialogEvents.ShowTrackerAlertDialog -> {
                TODO()
            }

            is TrackerDialogEvents.ShowTrackerNotStartedDialog -> {
                _trackerDialogState.value = _trackerDialogState.value.copy(
                    isDialogShown = true,
                    title = "Alert",
                    text = "You are not started a tracker yet",
                    confirmButtonText = "Ok",
                    dismissButtonText = "Cancel",
                    onConfirm = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                    },
                    onDismiss = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                    }
                )
            }

            is TrackerDialogEvents.ShowModuleChangeDialog -> {
                _trackerDialogState.value = _trackerDialogState.value.copy(
                    isDialogShown = true,
                    title = "Alert",
                    text = "Are you sure you want to stop tracker and change module?",
                    confirmButtonText = "Yes",
                    dismissButtonText = "No",
                    onConfirm = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
//                        updateSelectedTaskTime(trackerTime.value, idealTime.value)
                        resetTrackerTimer()
                        resetIdleTimer()
                        handleNavAction()
                        handleDropDownEvents(DropDownEvents.OnModuleSelection(trackerDialogEvents.selectedModule))
                    },
                    onDismiss = {
                        _trackerDialogState.value = _trackerDialogState.value.copy(
                            isDialogShown = false
                        )
                    }
                )
            }
        }
    }


    fun handleTrackerTimerEvents(timerEvents: TimerEvents) {
        when (timerEvents) {
            TimerEvents.FetchTime -> TODO()
            TimerEvents.PauseTimer -> TODO()
            TimerEvents.ResetTimer -> TODO()
            TimerEvents.StartTimer -> {
                if (trackerTime.value != 0 && isTrackerRunning.value) {
                    resumeTrackerTimer()
                    locationViewModel.resume()
                } else {
                    if (checkTaskAndProject()) {
                        startTrackerTimer()
                        locationViewModel.getCurrentLocation()
                        locationViewModel.startTracking()
                    }
                }
            }

            TimerEvents.StopTimer -> {
                updateSelectedTaskTime(trackerTime.value, idealTime.value)
                stopTrackerTimer()
                stopIdleTimer()
                locationViewModel.startTracking()
            }

            TimerEvents.UpdateTime -> TODO()

            TimerEvents.ResumeTimer -> {
                resumeTrackerTimer()
                locationViewModel.resume()
            }
        }
    }

    fun updateSelectedTaskTime(trackingTime: Int, idleTime: Int) {
        selectedTask.value?.time = trackingTime.toFloat()
        selectedTask.value?.unTrackedTime = idleTime.toFloat()
    }

    private fun checkTaskAndProject(): Boolean {
        if (_selectedModule.value == null) {
            _moduleDropDownState.value = _moduleDropDownState.value.copy(
                errorMessage = "Please select the a module"
            )
        }
        if (_selectedProject.value == null) {
            _projectDropDownState.value = _projectDropDownState.value.copy(
                errorMessage = "Please select the a project"
            )
        }
        if (_selectedTask.value == null) {
            _taskDropDownState.value = _taskDropDownState.value.copy(
                errorMessage = "Please select the a task"
            )
            return false
        }
        return true
    }


}


data class TrackerScreenData(
    val isSuccess: Boolean
)

sealed class DropDownEvents {
    data class OnModuleSearch(val inputText: String) : DropDownEvents()
    data class OnProjectSearch(val inputText: String) : DropDownEvents()
    data class OnTaskSearch(val inputText: String) : DropDownEvents()
    data class OnModuleSelection(val selectedModule: OrganisationModule) : DropDownEvents()
    data class OnProjectSelection(val selectedProject: Project) : DropDownEvents()
    data class OnTaskSelection(val selectedTask: TaskData) : DropDownEvents()
    data object OnProjectDropDownClick : DropDownEvents()
    data object OnTaskDropDownClick : DropDownEvents()
    data object OnModuleDropDownClick : DropDownEvents()
    data object OnProjectDismiss : DropDownEvents()
    data object OnTaskDismiss : DropDownEvents()
    data object OnModuleDismiss : DropDownEvents()
}

data class DropDownState<T>(
    val errorMessage: String = "",
    val inputText: String = "",
    val dropDownList: List<T> = emptyList<T>()
)


data class TrackerDialogState(
    val isDialogShown: Boolean = false,
    val title: String = "",
    val text: String = "",
    val confirmButtonText: String = "",
    val dismissButtonText: String? = "",
    var showDismissButton: Boolean = true,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit? = {}
)

sealed class TrackerDialogEvents {
    data object ShowExitDialog : TrackerDialogEvents()
    data object ShowIdleTimeDialog : TrackerDialogEvents()
    data class ShowModuleChangeDialog(val selectedModule: OrganisationModule) :
        TrackerDialogEvents()

    data class ShowProjectChangeDialog(val selectedProject: Project) : TrackerDialogEvents()
    data class ShowTaskChangeDialog(val selectedTask: TaskData) : TrackerDialogEvents()
    data object ShowTrackerNotStartedDialog : TrackerDialogEvents()

    //    data object showExitDialog : TrackerDialogEvents()
    //    data object showProjectChangeDialog : TrackerDialogEvents()
    //    data object showTaskChangeDialog : TrackerDialogEvents()
    // no confirmed design
    data object ShowTrackerAlertDialog : TrackerDialogEvents()
}


sealed class TimerEvents {
    data object StartTimer : TimerEvents()
    data object StopTimer : TimerEvents()
    data object ResetTimer : TimerEvents()
    data object PauseTimer : TimerEvents()
    data object ResumeTimer : TimerEvents()

    // ambitious
    data object UpdateTime : TimerEvents()
    data object FetchTime : TimerEvents()
}