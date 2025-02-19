package org.softsuave.bustlespot.organisation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bustlespot.composeapp.generated.resources.Res
import bustlespot.composeapp.generated.resources.compose_multiplatform
import bustlespot.composeapp.generated.resources.ic_logout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.navigation.Home
import org.softsuave.bustlespot.auth.utils.CustomAlertDialog
import org.softsuave.bustlespot.auth.utils.LoadingScreen
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.data.network.models.response.Organisation


@Composable
fun OrganisationScreen(
    modifier: Modifier = Modifier,
    navController: NavController,

    ) {
    val organisationViewModel = koinViewModel<OrganisationViewModel>()
    val uiEvent by organisationViewModel.uiEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val organisationList by organisationViewModel.organisationList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val logOutEvent by organisationViewModel.logOutEvent.collectAsState()
    val sessionManager: SessionManager = koinInject()

    val showLogOutDialog by organisationViewModel.showLogOutDialog.collectAsState()

    coroutineScope.launch {
        sessionManager.flowAccessToken.collectLatest { token ->
            sessionManager.setToken(token)
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BustleSpotAppBar(
                modifier = Modifier,
                title = {
                    Text(text = "Organisations")
                },
                onNavigationBackClick = {},
                isNavigationEnabled = false,
                isAppBarIconEnabled = true,
                iconUserName = "Test 1",
                isLogOutEnabled = true,
                onLogOutClick = {
                    organisationViewModel.showLogOutDialog()
                }
            )
        },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (showLogOutDialog) {
            CustomAlertDialog(
                title = "Log Out",
                text = "Are you sure you want to log out?",
                dismissButton = {
                    Button(
                        onClick = {
                            organisationViewModel.logOutDisMissed()
                        }, colors = ButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Red,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(5.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 5.dp,
                            focusedElevation = 7.dp,
                        )
                    ) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            organisationViewModel.performLogOut()
                        }, colors = ButtonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(5.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 5.dp,
                            focusedElevation = 7.dp,
                        )
                    ) {
                        Text("Logout")
                    }
                }
            )
        }
        when (uiEvent) {
            is UiEvent.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {

                    OrganizationList(
                        organizations = organisationList?.listOfOrganisations,
                        navController = navController
                    )
                }
            }

            is UiEvent.Failure -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        (uiEvent as UiEvent.Failure).error,
                        actionLabel = "Retry"
                    )
                }
            }

            UiEvent.Loading -> {
                LoadingScreen()
            }
        }
        when (logOutEvent) {
            is UiEvent.Failure -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        (logOutEvent as UiEvent.Failure).error,
                        actionLabel = "Retry"
                    )
                }
            }

            is UiEvent.Loading -> {
                LoadingScreen()
            }

            is UiEvent.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        (logOutEvent as UiEvent.Success).data.message,
                        actionLabel = "Retry"
                    )
                }

//                navController.navigate(
//                    route = Graph.AUTHENTICATION
//                ){
//                    popUpTo(Graph.HOME){
//                        inclusive = true
//                    }
//                    launchSingleTop = true
//                }

            }

            null -> {

            }
        }
    }


}

@Composable
fun OrganizationList(organizations: List<Organisation>?, navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        organizations?.let {
            items(organizations) { organization ->
                OrganizationItem(
                    logoResource = Res.drawable.compose_multiplatform,
                    organizationName = organization.name,
                    onClick = {
                        navController.navigate(
                            route = "${Home.Tracker.route}/{orgId}/{orgName}".replace(
                                oldValue = "{orgId}",
                                newValue = organization.organisationId.toString()
                            ).replace(
                                oldValue = "{orgName}",
                                newValue = organization.name
                            ),
                        )
                    }
                )
            }
        }
    }
    if (organizations?.isEmpty() == true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = "No Organization Found", modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Preview
@Composable
fun OrganizationItem(
    logoResource: DrawableResource,
    organizationName: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Image(
                painter = painterResource(resource = logoResource),
                contentDescription = "Organization Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = organizationName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Arrow Icon",
                tint = Color.Red
            )
        }
    }
}

@Composable
fun OrganisationCardIteam(modifier: Modifier = Modifier, organisationModel: Organisation) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().height(50.dp),
        colors = CardColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContentColor = Color.Unspecified,
            disabledContainerColor = Color.Unspecified
        ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            RoundedImageView(imageUrl = "organisationModel.organisationImageURL")
            Text(text = organisationModel.name, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun RoundedImageView(modifier: Modifier = Modifier, imageUrl: String = "URL") {
    Box(modifier = modifier.clip(CircleShape).size(40.dp)) {
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = "Icon image"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun BustleSpotAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    isNavigationEnabled: Boolean = false,
    onNavigationBackClick: () -> Unit,
    isAppBarIconEnabled: Boolean = false,
    iconUserName: String = "Demo",
    isLogOutEnabled: Boolean = false,
    onLogOutClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier.fillMaxWidth(),
        navigationIcon = {
            if (isNavigationEnabled) {
                IconButton(
                    onClick = {
                        onNavigationBackClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "back"
                    )
                }
            }
        },
        actions = {
            if (isLogOutEnabled) {
                IconButton(
                    onClick = {
                        onLogOutClick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.weight(.8f),
                        painter = painterResource(Res.drawable.ic_logout),
                        contentDescription = "logout"
                    )
                }
            }
            if (isAppBarIconEnabled) {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppBarIcon(
                        username = iconUserName
                    )
                }
            }
        },
        colors =
        TopAppBarColors(
            containerColor = Color.White,
            navigationIconContentColor = Color.Red,
            titleContentColor = Color.Red,
            scrolledContainerColor = Color.Unspecified,
            actionIconContentColor = Color.Unspecified
        )
    )
}

@Composable
fun AppBarIcon(
    modifier: Modifier = Modifier,
    username: String,
) {
    val showWord = username.split(" ").map { it[0].uppercase() }.joinToString("")
    Box(
        modifier = modifier.size(40.dp).border(
            width = 1.dp,
            shape = CircleShape,
            color = Color.Red,
        ).background(color = Color.Red.copy(alpha = .3f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = showWord,
            color = Color.Red,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )
    }
}