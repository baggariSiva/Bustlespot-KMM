package org.softsuave.bustlespot.tracker.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavHostController
import bustlespot.composeapp.generated.resources.Res
import bustlespot.composeapp.generated.resources.ic_drop_down
import bustlespot.composeapp.generated.resources.ic_drop_up
import bustlespot.composeapp.generated.resources.screen
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.softsuave.bustlespot.APP_VERSION
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.auth.utils.CustomAlertDialog
import org.softsuave.bustlespot.auth.utils.LoadingDialog
import org.softsuave.bustlespot.auth.utils.LoadingScreen
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.auth.utils.formatEpochToTime
import org.softsuave.bustlespot.auth.utils.secondsToTime
import org.softsuave.bustlespot.auth.utils.secondsToTimeForScreenshot
import org.softsuave.bustlespot.auth.utils.secondsToTimeFormat
import org.softsuave.bustlespot.browser.WebLinks.USER_ACTIVITY
import org.softsuave.bustlespot.browser.openWebLink
import org.softsuave.bustlespot.data.network.models.response.DisplayItem
import org.softsuave.bustlespot.data.network.models.response.Project
import org.softsuave.bustlespot.data.network.models.response.TaskData
import org.softsuave.bustlespot.locationmodule.LocationViewModel
import org.softsuave.bustlespot.locationmodule.sendLocalNotification
import org.softsuave.bustlespot.organisation.ui.BustleSpotAppBar
import org.softsuave.bustlespot.tracker.scheduleWork
import org.softsuave.bustlespot.utils.BustleSpotRed
import org.softsuave.bustlespot.utils.handleBackPress
import org.softsuave.bustlespot.utils.requestPermission
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun TrackerScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    organisationName: String,
    organisationId: String,
    onFocusReceived: () -> Unit = {}
) {
    val homeViewModel = koinViewModel<HomeViewModel>()

    // Tracker timer and other states from homeViewModel remain unchanged.
    val trackerTimer by homeViewModel.trackerTime.collectAsState()
    val isTrackerRunning by homeViewModel.isTrackerRunning.collectAsState()
    val idleTime by homeViewModel.idealTime.collectAsState()
    val screenShotState by homeViewModel.screenShotState.collectAsState()
    val screenShotTakenTime by homeViewModel.screenShotTakenTime.collectAsState()
    val customeTimeForIdleTime by homeViewModel.customeTimeForIdleTime.collectAsState()
//    val isNetworkAvailable by homeViewModel.isNetworkAvailable.collectAsState(false)
    // Collect the consolidated drop-down states from HomeViewModel.
    val projectDropDownState by homeViewModel.projectDropDownState.collectAsState()
    val taskDropDownState by homeViewModel.taskDropDownState.collectAsState()

    val trackerDialogState by homeViewModel.trackerDialogState.collectAsState()

    // Still track the selected project and task if needed.
    val selectedProject by homeViewModel.selectedProject.collectAsState()
    val selectedTask by homeViewModel.selectedTask.collectAsState()

    // UI event (loading, failure, etc.) from the view model.
    val uiEvent by homeViewModel.uiEvent.collectAsState()
    val dialogEvent by homeViewModel.dialogEvent.collectAsState()
    val lastSyncTime by homeViewModel.lastSyncTime.collectAsState()


    val totalIdleTime by homeViewModel.totalIdleTime.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val locationInfo by homeViewModel.locationInfo.collectAsState()


    LaunchedEffect(locationInfo) {
        sendLocalNotification(10, locationInfo)
    }


    // Launch idle dialog effect.
    LaunchedEffect(idleTime) {
        if (idleTime > customeTimeForIdleTime && !homeViewModel.trackerDialogState.value.isDialogShown) {
            onFocusReceived.invoke()
            homeViewModel.handleTrackerDialogEvents(trackerDialogEvents = TrackerDialogEvents.ShowIdleTimeDialog) {
                if (idleTime.seconds.inWholeMinutes.minutes < 120.minutes) {
                    homeViewModel.startPostingUntrackedActivity(
                        organisationId = organisationId.toInt()
                    )
                }
            }

//            showIdleDialog = true
            homeViewModel.stopTrackerTimer()
            homeViewModel.updateSelectedTaskTime(trackerTimer, idleTime)
            homeViewModel.updateTrackerTimer()
            homeViewModel.postUpdateActivity(
                organisationId = organisationId.toInt()
            )
        }
//        val now = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Kolkata"))
//        if (now.hour in 19 until 20 && now.minute in 0 until 1) {
//            navController.navigateUp()
//        }
    }

    DisposableEffect(Unit) {
        onDispose {
            homeViewModel.stopTrackerTimer()
            homeViewModel.stopIdleTimer()

        }
    }
// for mobile devices
    handleBackPress(
        onBack = {
            if (isTrackerRunning) {
                homeViewModel.handleTrackerDialogEvents(
                    TrackerDialogEvents.ShowExitDialog,
                    handleNavAction = {
                        homeViewModel.startPostingActivity(
                            organisationId = organisationId.toInt(),
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
                        organisationId = organisationId.toInt()
                    )
                }
                navController.navigateUp()
                Log.i("call receiveid")
            }
        )
    }
    LaunchedEffect(homeViewModel.canCallApi.value) {
        Log.d("isChanged ${homeViewModel.canCallApi.value}")
        if (homeViewModel.canCallApi.value) {
            homeViewModel.startPostingActivity(
                organisationId = organisationId.toInt()
            )
        } else {
            Log.d("call restored")
        }
    }
    LaunchedEffect(homeViewModel.canStoreApiCall.value) {
        Log.d("isChanged ${homeViewModel.canStoreApiCall.value}")
        if (homeViewModel.canStoreApiCall.value) {
            homeViewModel.storePostActivity(
                organisationId = organisationId.toInt()
            )
        } else {
            Log.d("call restored canStoreApiCall ${homeViewModel.canStoreApiCall.value}")
        }
    }
    // code for lunching the tracker not started dialog
    /*LaunchedEffect(Unit) {
        var dialogShown = false
        while (true) {
            delay(6000)
            if (!isTrackerRunning && !dialogShown) {
                homeViewModel.handleTrackerDialogEvents(
                    TrackerDialogEvents.ShowTrackerNotStartedDialog
                )
                dialogShown = true
            } else if (isTrackerRunning) {
                dialogShown = false
            }
        }
    }*/



    LaunchedEffect(key1 = Unit) {
        homeViewModel.getAllProjects(
            organisationId = organisationId
        )
    }

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
                                    organisationId = organisationId.toInt(),
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is UiEvent.Loading -> {
                    LoadingScreen()
                }

                is UiEvent.Success -> {
                    if (dialogEvent) {
                        LoadingDialog(dialogLoadingText = "Syncing working/idle time")
                    }
                    LazyColumn {
                        item {
                            DropDownSelectionList(
                                title = "Project",
                                dropDownList = projectDropDownState.dropDownList,
                                onItemClick = { selectedItem ->
                                    // true and true ->
                                    // true and false
                                    if (isTrackerRunning && selectedItem != selectedProject) {
                                        homeViewModel.handleTrackerDialogEvents(
                                            TrackerDialogEvents.ShowProjectChangeDialog(
                                                selectedItem as Project
                                            ), handleNavAction = {
                                                if (isTrackerRunning) {
                                                    homeViewModel.startPostingActivity(
                                                        organisationId.toInt()
                                                    )
                                                }
                                            }
                                        )
                                    } else if (selectedItem != selectedProject) {
                                        homeViewModel.handleDropDownEvents(
                                            DropDownEvents.OnProjectSelection(selectedItem as Project)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.85f).padding(vertical = 8.dp),
                                error = projectDropDownState.errorMessage,
                                onDropDownClick = {
                                    homeViewModel.handleDropDownEvents(DropDownEvents.OnProjectDropDownClick)
                                },
                                inputText = projectDropDownState.inputText,
                                onSearchText = { searchText ->
                                    homeViewModel.handleDropDownEvents(
                                        DropDownEvents.OnProjectSearch(
                                            searchText
                                        )
                                    )
                                },
                                onNoOptionClick = {
//                            if (selectedProject == null) {
                                    homeViewModel.handleDropDownEvents(
                                        DropDownEvents.OnProjectSearch(
                                            ""
                                        )
                                    )
//                            } else {
//                                homeViewModel.handleDropDownEvents(
//                                    DropDownEvents.OnProjectSelection(
//                                        selectedProject = selectedProject!!
//                                    )
//                                )
//                            }
                                },
                                selectedProject = selectedProject,
                                isSelected = selectedProject != null,
                                onDissmissClick = {
                                    homeViewModel.handleDropDownEvents(DropDownEvents.OnProjectDismiss)
                                }
                            )

                        }
                        item {
                            DropDownSelectionList(
                                title = "Task",
                                dropDownList = taskDropDownState.dropDownList,
                                onItemClick = { selectedItem ->
                                    if (isTrackerRunning && selectedItem != selectedTask) {
                                        homeViewModel.handleTrackerDialogEvents(
                                            TrackerDialogEvents.ShowTaskChangeDialog(
                                                selectedItem as TaskData
                                            ), handleNavAction = {
                                                if (isTrackerRunning) {
                                                    homeViewModel.startPostingActivity(
                                                        organisationId.toInt()
                                                    )
                                                }
                                            }
                                        )
                                    } else if (selectedItem != selectedTask) {
                                        homeViewModel.handleDropDownEvents(
                                            DropDownEvents.OnTaskSelection(selectedItem as TaskData)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.85f).padding(vertical = 8.dp),
                                error = taskDropDownState.errorMessage,
                                isEnabled = taskDropDownState.dropDownList.isNotEmpty(),
                                onDropDownClick = {
                                    homeViewModel.handleDropDownEvents(DropDownEvents.OnTaskDropDownClick)
                                },
                                onNoOptionClick = {
//                            if (selectedTask == null) {
                                    homeViewModel.handleDropDownEvents(DropDownEvents.OnTaskSearch(""))
//                            } else {
//                                homeViewModel.handleDropDownEvents(
//                                    DropDownEvents.OnTaskSelection(
//                                        selectedTask = selectedTask!!
//                                    )
//                                )
//                            }
                                },
                                inputText = taskDropDownState.inputText,
                                onSearchText = { searchText ->
                                    homeViewModel.handleDropDownEvents(
                                        DropDownEvents.OnTaskSearch(
                                            searchText
                                        )
                                    )
                                },
                                selectedTask = selectedTask,
                                isSelected = selectedTask != null,
                                onDissmissClick = {
                                    homeViewModel.handleDropDownEvents(DropDownEvents.OnTaskDismiss)
                                }
                            )
                        }
                        item {
                            TimerSessionSection(
                                trackerTimer = trackerTimer,
                                homeViewModel = homeViewModel,
                                idleTime = totalIdleTime,
                                isTrackerRunning = isTrackerRunning,
                                taskName = selectedTask?.name ?: "",
                                organisationId = organisationId
                            )
                        }
                        item {
                            ScreenShotSection(
                                lastImageTakenTime = secondsToTimeForScreenshot(screenShotTakenTime),
                                imageBitmap = screenShotState,
                                lastTakenImage = selectedTask?.screenshots
                            )
                        }
                        item {
                            SyncNowSection(
                                onClickUserActivity = {
                                    openWebLink(USER_ACTIVITY)
                                },
                                onClickSyncNow = {
                                    homeViewModel.startPostingActivity(
                                        organisationId = organisationId.toInt()
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


            /*
                        Box {
                            Row {
                                TextField(
                                    value = customeTimeForIdleTime.toString(),
                                    onValueChange = {
                                        if (it.isNotEmpty()) {
                                            homeViewModel.addCustomTimeForIdleTime(it.toInt())
                                        } else {
                                            homeViewModel.addCustomTimeForIdleTime(10)
                                        }
                                    },
                                    label = { Text("Custom Time") },
                                )
                            }
                        }*/

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
                            Text(trackerDialogState.dismissButtonText)
                        }
                    })
            }
        }
    }
}

@Composable
fun DropDownSelectionList(
    modifier: Modifier = Modifier,
    title: String,
    onSearchText: (String) -> Unit,
    inputText: String,
    dropDownList: List<DisplayItem>,
    onItemClick: (DisplayItem) -> Unit,
    isEnabled: Boolean = true,
    onDropDownClick: () -> Unit = {},
    onNoOptionClick: () -> Unit = {},
    error: String? = null,
    selectedProject: Project? = null,
    selectedTask: TaskData? = null,
    isSelected: Boolean = false,
    onDissmissClick: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    // We also track whether we've already notified the parent for this open.
    var hasNotifiedOnOpen by remember { mutableStateOf(false) }

    // Log state changes only when isMenuExpanded changes.
    LaunchedEffect(isMenuExpanded) {
        println("isMenuExpanded changed to: $isMenuExpanded")
    }
    val mutableInteractionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    val isFocused = mutableInteractionSource.collectIsPressedAsState()
    LaunchedEffect(isFocused.value) {
        Log.d("isFocused ${isFocused.value}")
        if (isFocused.value) {
            isMenuExpanded = true
            hasNotifiedOnOpen = true
        }
    }

    val screenWidth = remember(density) {
        with(density) { 600.dp.toPx() } // Default fallback width (adjust for desktop/iOS)
    }

    val maxHeight = with(density) { (screenWidth * 0.4f).toDp() } // Max width = 40% of screen
    val filteredList by remember(
        inputText,
        dropDownList,
        isSelected,
        selectedProject,
        selectedTask
    ) {
        derivedStateOf {
            if (!isSelected) {
                dropDownList.filter {
                    when (it) {
                        is Project -> it.name.contains(inputText, ignoreCase = true)
                        is TaskData -> it.name.contains(inputText, ignoreCase = true)
                        else -> true
                    }
                }
            } else {
                when (dropDownList.firstOrNull()) {
                    is Project -> dropDownList.moveToFirst(selectedProject)
                    is TaskData -> dropDownList.moveToFirst(selectedTask)
                    else -> dropDownList
                }
            }
        }
    }

    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = inputText,
            onValueChange = {
                onSearchText(it)
                isMenuExpanded = true
                hasNotifiedOnOpen = true
            },
            interactionSource = mutableInteractionSource,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onDropDownClick()
                        if (!isMenuExpanded && isEnabled) {
                            isMenuExpanded = true
                            // Only call onDropDownClick if we haven't already for this open cycle.
                            if (!hasNotifiedOnOpen) {
                                hasNotifiedOnOpen = true
                            }
                        } else {
                            // Close the dropdown and reset our notification flag.
                            isMenuExpanded = false
                            hasNotifiedOnOpen = false
                        }
                        println("icon clicked")
                    }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isMenuExpanded && hasNotifiedOnOpen && isEnabled) Res.drawable.ic_drop_up else Res.drawable.ic_drop_down
                        ), contentDescription = "Toggle Dropdown"
                    )
                }
            },
            label = {
                Text(
                    text = title, color = BustleSpotRed, modifier = Modifier.fillMaxWidth()
                )
            },
            supportingText = {
                if (error?.isNotEmpty() == true) {
                    Text(text = error, color = BustleSpotRed)
                }
            },
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = BustleSpotRed,
            ),
            isError = error?.isNotEmpty() ?: false
        )
        DropdownMenu(
            expanded = isMenuExpanded && hasNotifiedOnOpen && isEnabled,
            onDismissRequest = {
                isMenuExpanded = true
                hasNotifiedOnOpen = false
                onDissmissClick()
                println("dismiss called")
            },
            modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = maxHeight),
            properties = PopupProperties(focusable = false),
            containerColor = Color.White
        ) {
            if (filteredList.isNotEmpty()) {
                filteredList.forEach { item ->
                    when (item) {
                        is Project -> {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = item.name, modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onItemClick(item)
                                },
                                modifier = Modifier.background(
                                    if (item == selectedProject) BustleSpotRed.copy(alpha = 0.2f) else Color.White
                                ).pointerHoverIcon(PointerIcon.Hand),
                            )
                        }


                        is TaskData -> {
                            DropdownMenuItem(
                                text = {
                                    Text(text = item.name, modifier = Modifier.fillMaxWidth())
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onItemClick(item)
                                },
                                modifier = Modifier.background(
                                    if (item == selectedTask) BustleSpotRed.copy(alpha = 0.2f) else Color.White
                                ).pointerHoverIcon(PointerIcon.Hand)
                            )
                        }
                    }
                }
            } else {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "No Options",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Gray
                        )
                    }, onClick = {
                        isMenuExpanded = false
                        onNoOptionClick()
                    }, modifier = Modifier.background(Color.White)
                )
            }
        }
    }
}


@Composable
fun TimerSessionSection(
    modifier: Modifier = Modifier,
    taskName: String = "task",
    trackerTimer: Int,
    homeViewModel: HomeViewModel,
    idleTime: Int,
    isTrackerRunning: Boolean,
    organisationId: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.fillMaxWidth(0.85f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Current Session",
                fontSize = 15.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = taskName,
                color = BustleSpotRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp)) // Adds spacing between task name and timer
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isTrackerRunning) {
                    StopButton(onClick = {
                        requestPermission {
                            if (isTrackerRunning) {
                                homeViewModel.handleTrackerTimerEvents(TimerEvents.StopTimer)
                                homeViewModel.startPostingActivity(
                                    organisationId.toInt(),
                                    showLoading = true
                                )
                            } else {
                                homeViewModel.handleTrackerTimerEvents(TimerEvents.StartTimer)
                            }
                        }
                    })
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = BustleSpotRed,
                        modifier = Modifier
                            .size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                role = Role.Button,
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                requestPermission {
                                    if (isTrackerRunning) {
                                        homeViewModel.handleTrackerTimerEvents(TimerEvents.StopTimer)
                                        homeViewModel.startPostingActivity(organisationId.toInt())
                                    } else {
                                        homeViewModel.handleTrackerTimerEvents(TimerEvents.StartTimer)
                                    }
                                }
                            }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = secondsToTime(trackerTimer),
                    color = Color.Black,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IdleTime",
                color = BustleSpotRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = secondsToTimeFormat(idleTime),
                color = Color.Black,
            )
        }
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .background(BustleSpotRed, CircleShape).pointerHoverIcon(PointerIcon.Hand)

    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2500
                    0f at 0
                    4f at 2500
                },
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2500
                    1f at 0
                    0f at 2500
                },
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(16.dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.alpha = alpha
                }
                .background(BustleSpotRed, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(10.dp)
                .align(Alignment.Center)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun ScreenShotSection(
    modifier: Modifier = Modifier,
    lastImageTakenTime: String = "10min ago",
    imageBitmap: ImageBitmap? = imageResource(Res.drawable.screen),
    lastTakenImage: String? = ""
) {
    Column(
        modifier = modifier.fillMaxWidth(0.85f).padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latest Screen Capture",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = lastImageTakenTime,
                color = BustleSpotRed,
                style = MaterialTheme.typography.labelSmall
            )
        }
        imageBitmap?.let { bitmap ->
            Image(
                modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                    .aspectRatio(1.8f),
                bitmap = bitmap,
                contentDescription = "Screenshot"
            )
        } ?: AsyncImage(
            model = lastTakenImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            imageLoader = ImageLoader(LocalPlatformContext.current),
            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                .aspectRatio(1.8f)
        )
    }
}


@Composable
fun SyncNowSection(
    modifier: Modifier = Modifier,
    lastSyncTime: String = "11:50",
    onClickUserActivity: () -> Unit = {},
    onClickSyncNow: () -> Unit = {}
) {
    val syncNowInteractionSource = remember { MutableInteractionSource() }
    val isHovered by syncNowInteractionSource.collectIsHoveredAsState()
    val userActivityInteractionSource = remember { MutableInteractionSource() }
    val isHoveredOne by userActivityInteractionSource.collectIsHoveredAsState()
    Column(
        modifier = modifier.fillMaxWidth(0.85f).padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            Text(
                text = "Synced @ $lastSyncTime. Syncs every 10 minutes.",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Sync Now",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = syncNowInteractionSource,
                    indication = null,
                    enabled = true,
                    onClickLabel = "clicked",
                    role = Role.Button
                ) {
                    onClickSyncNow()
                }.pointerHoverIcon(PointerIcon.Hand),
                color = if (isHovered) BustleSpotRed else Color.Black,
            )
        }
        Text(
            text = "User Activity",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(
                interactionSource = userActivityInteractionSource,
                indication = null,
                enabled = true,
                onClickLabel = "clicked",
                role = Role.Button
            ) {
                onClickUserActivity()
            }.pointerHoverIcon(PointerIcon.Hand),
            color = if (isHoveredOne) BustleSpotRed else Color.Black,
        )
    }
}

fun <T> List<T>.moveToFirst(item: T): List<T> {
    val mutableList = this.toMutableList()
    if (mutableList.remove(item)) { // Only move if the item exists
        mutableList.add(0, item)
    }
    return mutableList
}