package org.softsuave.bustlespot.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.softsuave.bustlespot.APP_VERSION
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.PlatFormType
import org.softsuave.bustlespot.auth.utils.CustomAlertDialog
import org.softsuave.bustlespot.auth.utils.LoadingDialog
import org.softsuave.bustlespot.auth.utils.LoadingScreen
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.auth.utils.formatEpochToTime
import org.softsuave.bustlespot.auth.utils.secondsToTime
import org.softsuave.bustlespot.auth.utils.secondsToTimeForScreenshot
import org.softsuave.bustlespot.browser.WebLinks.USER_ACTIVITY
import org.softsuave.bustlespot.browser.openWebLink
import org.softsuave.bustlespot.data.network.models.response.OrganisationModule
import org.softsuave.bustlespot.data.network.models.response.Project
import org.softsuave.bustlespot.data.network.models.response.TaskData
import org.softsuave.bustlespot.organisation.ui.BustleSpotAppBar
import org.softsuave.bustlespot.tracker.di.trackerDiModule
import org.softsuave.bustlespot.tracker.scheduleWork
import org.softsuave.bustlespot.tracker.ui.model.DropDownSelectionData
import org.softsuave.bustlespot.utils.BustleSpotRed
import org.softsuave.bustlespot.utils.HandleBackPress
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun TrackerScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    organisationName: String,
    organisationId: String,
    onFocusReceived: () -> Unit = {},
    homeViewModel: HomeViewModel = koinViewModel()
) {
    val trackerTimer by homeViewModel.trackerTime.collectAsState()
    val isTrackerRunning by homeViewModel.isTrackerRunning.collectAsState()
    val idleTime by homeViewModel.idealTime.collectAsState()
    val platformType by homeViewModel.platFormType.collectAsState()
    val screenShotState by homeViewModel.screenShotState.collectAsState()
    val screenShotTakenTime by homeViewModel.screenShotTakenTime.collectAsState()
    val customeTimeForIdleTime by homeViewModel.customeTimeForIdleTime.collectAsState()
    val moduleDropDownState by homeViewModel.moduleDropDownState.collectAsState()
    val projectDropDownState by homeViewModel.projectDropDownState.collectAsState()
    val taskDropDownState by homeViewModel.taskDropDownState.collectAsState()

    val trackerDialogState by homeViewModel.trackerDialogState.collectAsState()

    val canCallApi by homeViewModel.canCallApi.collectAsState()

    // Still track the selected project and task if needed.
    val selectedProject by homeViewModel.selectedProject.collectAsState()
    val selectedTask by homeViewModel.selectedTask.collectAsState()
    val selectedModule by homeViewModel.selectedModule.collectAsState()

    // UI event (loading, failure, etc.) from the view model.
    val uiEvent by homeViewModel.uiEvent.collectAsState()
    val dialogEvent by homeViewModel.dialogEvent.collectAsState()
    val lastSyncTime by homeViewModel.lastSyncTime.collectAsState()


    val totalIdleTime by homeViewModel.totalIdleTime.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val liveLocationCoordinate by homeViewModel.liveLocationCoordinate.collectAsState()

    val imageBitmap by homeViewModel.imageBitmap.collectAsState()

    LaunchedEffect(key1 = Unit) {
        homeViewModel.getAllModules(
            organisationId = organisationId
        )
    }



    LaunchedEffect(idleTime) {
        if (idleTime > customeTimeForIdleTime && !homeViewModel.trackerDialogState.value.isDialogShown) {
            onFocusReceived.invoke()
            homeViewModel.handleTrackerDialogEvents(trackerDialogEvents = TrackerDialogEvents.ShowIdleTimeDialog) {
                if (idleTime.seconds.inWholeMinutes.minutes < 120.minutes) {
                    homeViewModel.startPostingUntrackedActivity()
                }
            }
//            showIdleDialog = true
            homeViewModel.stopTrackerTimer()
            homeViewModel.updateSelectedTaskTime(trackerTimer, idleTime)
            homeViewModel.updateTrackerTimer()
            homeViewModel.postUpdateActivity()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            homeViewModel.stopTrackerTimer()
            homeViewModel.stopIdleTimer()

        }
    }
// for mobile devices
    HandleBackPress(
        onBack = {
            if (isTrackerRunning) {
                homeViewModel.handleTrackerDialogEvents(
                    TrackerDialogEvents.ShowExitDialog,
                    handleNavAction = {
                        homeViewModel.startPostingActivity(
                            showLoading = true
                        ) {
                            navController.navigateUp()
                        }
                    })
            } else {
                navController.navigateUp()
            }
        }
    )
    LaunchedEffect(key1 = Unit) {
        homeViewModel.checkAndPostActivities()
        scheduleWork(
            performTask = {
                if (isTrackerRunning) {
                    homeViewModel.startPostingActivity(
                    )
                }
                navController.navigateUp()
                Log.i("call receiveid")
            }
        )
    }
    LaunchedEffect(canCallApi) {
        Log.d("isChanged $canCallApi")
        if (canCallApi) {
            homeViewModel.startPostingActivity()
        } else {
            Log.d("call restored")
        }
    }
    LaunchedEffect(homeViewModel.canStoreApiCall.value) {
        Log.d("isChanged ${homeViewModel.canStoreApiCall.value}")
        if (homeViewModel.canStoreApiCall.value) {
            homeViewModel.storePostActivity()
        } else {
            Log.d("call restored canStoreApiCall ${homeViewModel.canStoreApiCall.value}")
        }
    }


    val moduleDropDownSelectionData =
        DropDownSelectionData<OrganisationModule>(
            title = "Module",
            onSearchText = { searchText ->
                homeViewModel.handleDropDownEvents(
                    DropDownEvents.OnModuleSearch(searchText)
                )
            },
            inputText = moduleDropDownState.inputText,
            dropDownList = moduleDropDownState.dropDownList,
            onItemClick = { selectedItem ->
                if (isTrackerRunning && selectedItem.moduleId != selectedModule?.moduleId) {
                    homeViewModel.handleTrackerDialogEvents(
                        TrackerDialogEvents.ShowModuleChangeDialog(selectedItem),
                        handleNavAction = {
                            if (isTrackerRunning) {
                                homeViewModel.startPostingActivity()
                            }
                        }
                    )
                } else if (selectedItem != selectedModule) {
                    homeViewModel.handleDropDownEvents(
                        DropDownEvents.OnModuleSelection(selectedItem)
                    )
                }
            },
            displayText = { module -> module.moduleName },
            isSelectedItem = { item, selected -> item == selected },
            isEnabled = moduleDropDownState.dropDownList.isNotEmpty(),
            onDropDownClick = {
                homeViewModel.handleDropDownEvents(DropDownEvents.OnModuleDropDownClick)
            },
            onNoOptionClick = {
                homeViewModel.handleDropDownEvents(
                    DropDownEvents.OnModuleSearch("")
                )
            },
            error = moduleDropDownState.errorMessage,
            selectedItem = selectedModule,
            isSelected = selectedModule != null,
            onDismissClick = {
                homeViewModel.handleDropDownEvents(DropDownEvents.OnModuleDismiss)
            },
            readOnly = PlatFormType.DESKTOP != platformType
        )

    val projectDropDownSelectionData = DropDownSelectionData<Project>(
        title = "Project",
        onSearchText = { searchText ->
            homeViewModel.handleDropDownEvents(
                DropDownEvents.OnProjectSearch(searchText)
            )
        },
        inputText = projectDropDownState.inputText,
        dropDownList = projectDropDownState.dropDownList,
        onItemClick = { selectedItem ->
            if (isTrackerRunning && selectedItem != selectedProject) {
                homeViewModel.handleTrackerDialogEvents(
                    TrackerDialogEvents.ShowProjectChangeDialog(selectedItem),
                    handleNavAction = {
                        if (isTrackerRunning) {
                            homeViewModel.startPostingActivity()
                        }
                    }
                )
            } else if (selectedItem != selectedProject) {
                homeViewModel.handleDropDownEvents(
                    DropDownEvents.OnProjectSelection(selectedItem)
                )
            }
        },
        displayText = { project -> project.name },
        isSelectedItem = { item, selected -> item == selected },
        isEnabled = projectDropDownState.dropDownList.isNotEmpty(),
        onDropDownClick = {
            homeViewModel.handleDropDownEvents(DropDownEvents.OnProjectDropDownClick)
        },
        onNoOptionClick = {
            homeViewModel.handleDropDownEvents(
                DropDownEvents.OnProjectSearch("")
            )
        },
        error = projectDropDownState.errorMessage,
        selectedItem = selectedProject,
        isSelected = selectedProject != null,
        onDismissClick = {
            homeViewModel.handleDropDownEvents(DropDownEvents.OnProjectDismiss)
        },
        readOnly = platformType != PlatFormType.DESKTOP
    )

    val taskDropDownSelectionData = DropDownSelectionData<TaskData>(
        title = "Task",
        onSearchText = { searchText ->
            homeViewModel.handleDropDownEvents(
                DropDownEvents.OnTaskSearch(searchText)
            )
        },
        inputText = taskDropDownState.inputText,
        dropDownList = taskDropDownState.dropDownList,
        onItemClick = { selectedItem ->
            if (isTrackerRunning && selectedItem != selectedTask) {
                homeViewModel.handleTrackerDialogEvents(
                    TrackerDialogEvents.ShowTaskChangeDialog(selectedItem),
                    handleNavAction = {
                        if (isTrackerRunning) {
                            homeViewModel.startPostingActivity()
                        }
                    }
                )
            } else if (selectedItem != selectedTask) {
                homeViewModel.handleDropDownEvents(
                    DropDownEvents.OnTaskSelection(selectedItem)
                )
            }
        },
        displayText = { task -> task.name },
        isSelectedItem = { item, selected -> item == selected },
        isEnabled = taskDropDownState.dropDownList.isNotEmpty(),
        onDropDownClick = {
            homeViewModel.handleDropDownEvents(DropDownEvents.OnTaskDropDownClick)
        },
        onNoOptionClick = {
            homeViewModel.handleDropDownEvents(DropDownEvents.OnTaskSearch(""))
        },
        error = taskDropDownState.errorMessage,
        selectedItem = selectedTask,
        isSelected = selectedTask != null,
        onDismissClick = {
            homeViewModel.handleDropDownEvents(DropDownEvents.OnTaskDismiss)
        },
        readOnly = platformType != PlatFormType.DESKTOP
    )

    fun getDropDownSelectionData() = listOf(
        moduleDropDownSelectionData,
        projectDropDownSelectionData,
        taskDropDownSelectionData
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BustleSpotAppBar(
                title = { Text(text = organisationName, color = BustleSpotRed) },
                onNavigationBackClick = {
                    if (isTrackerRunning) {
                        homeViewModel.handleTrackerDialogEvents(
                            TrackerDialogEvents.ShowExitDialog,
                            handleNavAction = {
                                homeViewModel.startPostingActivity(
                                    showLoading = true
                                ) {
                                    navController.navigateUp()
                                }
                            })
                    } else {
                        navController.navigateUp()
                    }
                },
                isNavigationEnabled = true,
                isAppBarIconEnabled = false, // to remove the user icon in tracker screen
                iconUserName = "Test 1",
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White,
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Handle UI events (failure, loading, success).
            when (uiEvent) {
                is UiEvent.Failure -> {
                    LaunchedEffect(uiEvent) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                (uiEvent as UiEvent.Failure).error, actionLabel = "Retry"
                            )
                        }
                    }
                    Text(
                        text = "Fetching data is failed due to ${(uiEvent as UiEvent.Failure).error}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is UiEvent.Loading -> {
                    LoadingScreen()
                }

                is UiEvent.Success -> {
                    if (dialogEvent) {
                        LoadingDialog(loadingTitleText = "Syncing working/idle time")
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(.85f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            for (dropDownData in getDropDownSelectionData()) {
                                DropDownSelectionList(
                                    modifier = Modifier,
                                    dropDownData
                                )
                            }
                        }
                        item {
                            TimerSessionSection(
                                trackerTimer = trackerTimer,
                                homeViewModel = homeViewModel,
                                idleTime = totalIdleTime,
                                isTrackerRunning = isTrackerRunning,
                                taskName = selectedTask?.name ?: "",
                                platFormType = platformType
                            )
                        }
                        item {
                            when (platformType) {
                                PlatFormType.IOS, PlatFormType.ANDROID -> {
                                    MapSection(
                                        modifier = Modifier,
                                        centerCoordinate = liveLocationCoordinate
                                    )
                                    ImageUploadSection(
                                        modifier = Modifier,
                                        imageBitmap = imageBitmap,
                                        addImageBytes = homeViewModel::addImageBytes
                                    )
                                }

                                PlatFormType.DESKTOP -> {
                                    ScreenShotSection(
                                        lastImageTakenTime = secondsToTimeForScreenshot(
                                            screenShotTakenTime
                                        ),
                                        imageBitmap = screenShotState,
                                        lastTakenImage = selectedTask?.lastScreenshot
                                    )
                                }

                                PlatFormType.UNKNOWN -> {
                                    Text("Platform is unknown")
                                }
                            }
                        }

                        item {
                            SyncNowSection(
                                onClickUserActivity = {
                                    openWebLink(USER_ACTIVITY)
                                },
                                onClickSyncNow = {
                                    homeViewModel.startPostingActivity(
                                    )
                                },
                                lastSyncTime = formatEpochToTime(lastSyncTime)
                            )
                        }

                    }
                }
            }

            Text(
                text = "v$APP_VERSION",
                color = Color.Black,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.End).padding(end = 5.dp, bottom = 5.dp)
            )


            if (trackerDialogState.isDialogShown) {
                CustomAlertDialog(
                    title = trackerDialogState.title,
                    text = trackerDialogState.text.replace(
                        "%s",
                        secondsToTime(homeViewModel.idealTime.value)
                    ),
                    confirmButton = {
                        TextButton(
                            onClick = {
                                trackerDialogState.onConfirm()
                            },
                            colors = ButtonColors(
                                containerColor = BustleSpotRed,
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(5.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 5.dp,
                                focusedElevation = 7.dp,
                            ),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(trackerDialogState.confirmButtonText)
                        }
                    },
                    dismissButton = {
                        if (trackerDialogState.showDismissButton) {
                            TextButton(
                                onClick = {
                                    trackerDialogState.onDismiss()
                                },
                                colors = ButtonColors(
                                    containerColor = Color.White,
                                    contentColor = BustleSpotRed,
                                    disabledContainerColor = Color.Gray,
                                    disabledContentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(5.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 5.dp,
                                    focusedElevation = 7.dp,
                                ),
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(trackerDialogState.dismissButtonText!!)
                            }
                        } else {
                            null
                        }
                    }
                )
            }
        }
    }

}


@Composable
@Preview()
fun Preview() {
//    val mockViewModel = HomeViewModel(
//
//    )
//    TrackerScreen(viewModel = mockViewModel)
//    TrackerScreen(navController = rememberNavController(), organisationName = "Test", organisationId = "1"){}
}











