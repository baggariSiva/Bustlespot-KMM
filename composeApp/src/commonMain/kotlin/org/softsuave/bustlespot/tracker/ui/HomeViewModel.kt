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
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.auth.utils.timeStringToSeconds
import org.softsuave.bustlespot.data.network.models.request.UpdateActivityRequest
import org.softsuave.bustlespot.data.network.models.response.DisplayItem
import org.softsuave.bustlespot.data.network.models.response.Project
import org.softsuave.bustlespot.data.network.models.response.TaskData
import org.softsuave.bustlespot.network.NetworkMonitor
import org.softsuave.bustlespot.timer.TrackerModule
import org.softsuave.bustlespot.tracker.data.TrackerRepository
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import org.softsuave.bustlespot.tracker.data.model.PostActivityRequest
import org.softsuave.bustlespot.tracker.ui.model.GetTasksRequest

class HomeViewModel(
    private val sessionManager: SessionManager,
    private val trackerRepository: TrackerRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {


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
//    val isNetworkAvailable: Flow<Boolean> = networkMonitor.isConnected

    fun startTrackerTimer() = trackerModule.startTimer()

    private fun constructPostActivityRequest(
        organisationId: Int,
        activityDataOfModule: ActivityData
    ): PostActivityRequest {
//        val taskId = _selectedTask.value?.taskId ?: 0
//        val projectId = _selectedProject.value?.projectId ?: 0
//        //TODO("Need to work on startTime and endTime")
//        val startTime = activityDataOfModule.startTime
//        val endTime = activityDataOfModule.endTime
//        val mouseActivity =activityDataOfModule.mouseActivity
//        val keyboardActivity = keyboradKeyEvents.value
//        val totalActivity = mouseActivity + keyboardActivity
//        val notes = _selectedTask.value?.notes ?: "Activity recorded"
//        val uri = ""
//        val unTrackedTime = _totalIdleTime.value.toLong()
//        val activityData = ActivityData(
//            taskId = taskId,
//            projectId = projectId,
//            startTime = startTime,
//            endTime = endTime,
//            mouseActivity = mouseActivity,
//            keyboardActivity = keyboardActivity,
//            totalActivity = totalActivity,
//            billable = "",
//            notes = notes,
//            orgId = organisationId,
//            uri = uri,
//            unTrackedTime = unTrackedTime
//        )

        activityDataOfModule.apply {
            this.taskId = _selectedTask.value?.taskId ?: 0
            this.projectId = _selectedProject.value?.projectId ?: 0
            this.orgId = organisationId
        }
        return PostActivityRequest(activityData = arrayListOf(activityDataOfModule))
    }

    fun startPostingActivity(
        organisationId: Int,
        showLoading: Boolean = false,
        doActionOnSuccess: () -> Unit = {}
    ) {
        try {
            val request = constructPostActivityRequest(
                organisationId,
                activityDataOfModule = trackerModule.getActivityData()
            )
            Log.d("$request----reguest")
            postUserActivity(request, showLoading, doActionOnSuccess)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun storePostActivity(
        organisationId: Int
    ) {
        try {
            val request = constructPostActivityRequest(
                organisationId,
                activityDataOfModule = trackerModule.getStoreActivityData()
            )
            canStoreApiCall.value = !trackerRepository.storePostUserActivity(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startPostingUntrackedActivity(
        organisationId: Int,
    ) {
        try {
            val request = constructPostActivityRequest(
                organisationId,
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
//    fun startScreenshotTask() = trackerModule.startScreenshotTask()
//    fun pauseScreenshotTask() = trackerModule.pauseScreenshotTask()
//    fun resumeScreenshotTask() = trackerModule.resumeScreenshotTask()
//    fun stopScreenshotTask() = trackerModule.stopScreenshotTask()

    fun resetIdleTimer() = trackerModule.resetIdleTimer()
    fun updateTrackerTimer() = trackerModule.updateTrackerTimer()
//    fun addCustomTimeForIdleTime(time: Int) = trackerModule.addCustomTimeForIdleTime(time)


    fun stopIdleTimer() = trackerModule.stopIdleTimer()


    //    private val _taskList =MutableStateFlow<List<TaskData>>(emptyList())
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

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskData?>(null)
    val selectedTask: StateFlow<TaskData?> = _selectedTask.asStateFlow()

    private val _projectDropDownState = MutableStateFlow(DropDownState())
    val projectDropDownState: StateFlow<DropDownState> = _projectDropDownState.asStateFlow()

    private val _taskDropDownState = MutableStateFlow(DropDownState())
    val taskDropDownState: StateFlow<DropDownState> = _taskDropDownState.asStateFlow()

    private val _trackerDialogState: MutableStateFlow<TrackerDialogState> =
        MutableStateFlow(TrackerDialogState())
    val trackerDialogState: StateFlow<TrackerDialogState> = _trackerDialogState.asStateFlow()

    private val _totalIdleTime: MutableStateFlow<Int> = MutableStateFlow(0)
    val totalIdleTime: StateFlow<Int> = _totalIdleTime.asStateFlow()

    // actual is 7200ms -- 120 mins
    private val idealTimeThreshold: Int = 7200

    fun getAllProjects(organisationId: String) {
        viewModelScope.launch {
            trackerRepository.getAllProjects(organisationId).collect { result ->
                when (result) {
                    is Result.Error -> {
                        _uiEvent.value = UiEvent.Failure(result.message ?: "Unknown Error")
                        _projectDropDownState.value = _projectDropDownState.value.copy(
                            errorMessage = result.message ?: "Failed to fetch projects"
                        )
                    }

                    Result.Loading -> {
                        _uiEvent.value = UiEvent.Loading
                    }

                    is Result.Success -> {
                        val filteredList = result.data.projectLists?.filter { project ->
                            project.users?.any { it.userId == sessionManager.userId } == true
                        } ?: emptyList()

                        _mainProjectList.value = filteredList
                        _projectDropDownState.value = _projectDropDownState.value.copy(
                            dropDownList = filteredList,
                            errorMessage = if (result.data.projectLists.isNullOrEmpty()) "No projects to select" else ""
                        )
                        trackerScreenData.is_success
                        _uiEvent.update { UiEvent.Success(trackerScreenData) }
                        fetchAllTasksForProjects(
                            projects = filteredList,
                            organisationId
                        )
                    }
                }
            }
        }
    }


//    fun fetchTasksForProject(projectId: Int, organisationId: String) {
//        viewModelScope.launch {
//            trackerRepository.getAllTask(
//                GetTasksRequest(projectId = projectId.toString(), organisationId)
//            ).collect { result ->
//                when (result) {
//                    is Result.Error -> {
//                        _taskDropDownState.value = _taskDropDownState.value.copy(
//                            errorMessage = result.message ?: "Failed to fetch tasks"
//                        )
//                    }
//
//                    is Result.Loading -> {
//                        Log.d("Loading at projects")
//                    }
//
//                    is Result.Success -> {
//                        val taskList = result.data.taskDetails ?: emptyList()
//                        _mainTaskList.value = _mainTaskList.value.plus(taskList)
//                        trackerScreenData.boolen
//                    }
//                }
//            }
//        }
//    }
//

    private fun fetchAllTasksForProjects(projects: List<Project>, organisationId: String) {
        viewModelScope.launch {
            projects.forEach { project ->
                trackerRepository.getAllTask(
                    GetTasksRequest(projectId = project.projectId.toString(), organisationId)
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
                            trackerScreenData.is_success
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
        postActivityRequest: PostActivityRequest, showLoading: Boolean = false,
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
            }

            is DropDownEvents.OnProjectSelection -> {
                _selectedProject.value = dropDownEvents.selectedProject
                _projectDropDownState.value = _projectDropDownState.value.copy(
                    inputText = dropDownEvents.selectedProject.name,
                    errorMessage = ""
                )

                val filteredTasks =
                    _mainTaskList.value.filter { it.projectId == dropDownEvents.selectedProject.projectId }
                _taskDropDownState.value = _taskDropDownState.value.copy(
                    dropDownList = filteredTasks,
                    errorMessage = if (filteredTasks.isEmpty()) "No task available to select" else "",
                    inputText = ""
                )
                _selectedTask.value = null
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
                    dropDownEvents.selectedTask.time ?: 0,
                    dropDownEvents.selectedTask.unTrackedTime ?: 0
                )
                trackerModule.setLastScreenShotTime(
                    dropDownEvents.selectedTask.lastScreenShotTime?.timeStringToSeconds() ?: 0
                )
                _totalIdleTime.value = dropDownEvents.selectedTask.unTrackedTime ?: 0
            }

            is DropDownEvents.OnProjectDropDownClick -> {
                println("Project dropdown clicked")
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
        }
    }


    fun handleTrackerDialogEvents(
        trackerDialogEvents: TrackerDialogEvents,
        handleNavAction: () -> Unit = {}
    ) {
        when (trackerDialogEvents) {
            TrackerDialogEvents.ShowExitDialog -> {
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

            TrackerDialogEvents.ShowTrackerAlertDialog -> {
                TODO()
            }

            TrackerDialogEvents.ShowTrackerNotStartedDialog -> {
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
                } else {
                    if (checkTaskAndProject()) {
                        startTrackerTimer()
                    }
                }
            }

            TimerEvents.StopTimer -> {
                updateSelectedTaskTime(trackerTime.value, idealTime.value)
                stopTrackerTimer()
                stopIdleTimer()
            }

            TimerEvents.UpdateTime -> TODO()

            TimerEvents.ResumeTimer -> {
                resumeTrackerTimer()
            }
        }
    }

    fun updateSelectedTaskTime(trackingTime: Int, idleTime: Int) {
        selectedTask.value?.time = trackingTime
        selectedTask.value?.unTrackedTime = idleTime
    }

    private fun checkTaskAndProject(): Boolean {
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
    val is_success: Boolean
//    val listOfProject: MutableList<Project>?,
//    val listOfTask: MutableList<TaskData>?
)

sealed class DropDownEvents {
    data class OnProjectSearch(val inputText: String) : DropDownEvents()
    data class OnTaskSearch(val inputText: String) : DropDownEvents()
    data class OnProjectSelection(val selectedProject: Project) : DropDownEvents()
    data class OnTaskSelection(val selectedTask: TaskData) : DropDownEvents()
    data object OnProjectDropDownClick : DropDownEvents()
    data object OnTaskDropDownClick : DropDownEvents()
    data object OnProjectDismiss : DropDownEvents()
    data object OnTaskDismiss : DropDownEvents()
}

data class DropDownState(
    val errorMessage: String = "",
    val inputText: String = "",
    val dropDownList: List<DisplayItem> = emptyList()
)


data class TrackerDialogState(
    val isDialogShown: Boolean = false,
    val title: String = "",
    val text: String = "",
    val confirmButtonText: String = "",
    val dismissButtonText: String = "",
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

sealed class TrackerDialogEvents {
    data object ShowExitDialog : TrackerDialogEvents()
    data object ShowIdleTimeDialog : TrackerDialogEvents()
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