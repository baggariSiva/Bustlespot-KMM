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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import bustlespot.composeapp.generated.resources.Res
import bustlespot.composeapp.generated.resources.ic_drop_down
import bustlespot.composeapp.generated.resources.ic_drop_up
import bustlespot.composeapp.generated.resources.ic_password_visible
import bustlespot.composeapp.generated.resources.screen
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.auth.utils.secondsToTime
import org.softsuave.bustlespot.auth.utils.secondsToTimeFormat
import org.softsuave.bustlespot.tracker.ui.model.DropDownSelectionData
import org.softsuave.bustlespot.utils.BustleSpotRed
import org.softsuave.bustlespot.utils.moveToFirst

@Composable
fun <T> DropDownSelectionList(
    modifier: Modifier = Modifier,
    data: DropDownSelectionData<T>
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val mutableInteractionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    val isFocused by mutableInteractionSource.collectIsPressedAsState()
    LaunchedEffect(isFocused) {
        if (isFocused) {
            isMenuExpanded = true
        }
        Log.d("isFocused $isFocused")
    }

    // Compute max height once based on density
    val maxHeight by remember(density) {
        derivedStateOf { with(density) { (600.dp.toPx() * 0.4f).toDp() } }
    }

    // Optimize filtered list computation
    val filteredList by remember(
        data.inputText,
        data.dropDownList,
        data.isSelected,
        data.selectedItem
    ) {
        derivedStateOf {
            if (!data.isSelected) {
                data.dropDownList.filter {
                    data.displayText(it).contains(data.inputText, ignoreCase = true)
                }
            } else {
                data.dropDownList.moveToFirst(data.selectedItem)
            }
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = data.inputText,
            onValueChange = {
                data.onSearchText(it)
                isMenuExpanded = true
            },
            interactionSource = mutableInteractionSource,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f),
            trailingIcon = {
                IconButton(
                    onClick = {
                        data.onDropDownClick()
                        isMenuExpanded = !(isMenuExpanded && data.isEnabled)
//                        isMenuExpanded = if (data.isEnabled) !isMenuExpanded else false
                        println("icon clicked")
                    },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isMenuExpanded && data.isEnabled) Res.drawable.ic_drop_up else Res.drawable.ic_drop_down
                        ),
                        contentDescription = "Toggle Dropdown"
                    )
                }
            },
            label = { Text(data.title, color = BustleSpotRed, modifier = Modifier.fillMaxWidth()) },
            supportingText = {
                data.error?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, color = BustleSpotRed)
                }
            },
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = BustleSpotRed
            ),
            isError = !data.error.isNullOrEmpty(),
            readOnly = data.readOnly
        )
        DropdownMenu(
            expanded = isMenuExpanded && data.isEnabled,
            onDismissRequest = {
                isMenuExpanded = false
                data.onDismissClick()
                println("dismiss called")
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = maxHeight),
            properties = PopupProperties(focusable = false),
            containerColor = Color.White
        ) {
            if (filteredList.isNotEmpty()) {
                filteredList.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(data.displayText(item), modifier = Modifier.fillMaxWidth()) },
                        onClick = {
                            isMenuExpanded = false
                            data.onItemClick(item)
                        },
                        modifier = Modifier
                            .background(
                                if (data.isSelectedItem(
                                        item!!,
                                        data.selectedItem
                                    )
                                ) BustleSpotRed.copy(alpha = 0.2f)
                                else Color.White
                            )
                            .pointerHoverIcon(PointerIcon.Hand)
                    )
                }
            } else {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No Options",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Gray
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        data.onNoOptionClick()
                    },
                    modifier = Modifier.background(Color.White)
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
//                        requestPermission {
                        if (isTrackerRunning) {
                            homeViewModel.handleTrackerTimerEvents(TimerEvents.StopTimer)
                            homeViewModel.startPostingActivity(
                                showLoading = true
                            )
                        } else {
                            homeViewModel.handleTrackerTimerEvents(TimerEvents.StartTimer)
                        }
//                        }
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
//                                requestPermission {
                                if (isTrackerRunning) {
                                    homeViewModel.handleTrackerTimerEvents(TimerEvents.StopTimer)
                                    homeViewModel.startPostingActivity()
                                } else {
                                    homeViewModel.handleTrackerTimerEvents(TimerEvents.StartTimer)
                                }
//                                }
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
fun MapSection(
    modifier: Modifier = Modifier,
    centerCoordinate: Coordinate,
    onMarkerClick: (Coordinate) -> Unit = {}
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
                text = "Latest Location Capture",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        MapViewMobile(
            centerCoordinate = centerCoordinate,
            onMarkerClick = onMarkerClick
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

@Composable
fun UploadImageSection(
    modifier: Modifier = Modifier,
    onClickUploadImage: () -> Unit = {},
    imageBitmap: ImageBitmap? = null,
    imageLoader: ImageLoader? = null
) {

}


// composeApp/src/commonMain/kotlin/MapView.kt
data class Coordinate(val latitude: Double, val longitude: Double)

@Composable
expect fun MapViewMobile(
    centerCoordinate: Coordinate,
    modifier: Modifier = Modifier,
    onMarkerClick: (Coordinate) -> Unit = {}
)