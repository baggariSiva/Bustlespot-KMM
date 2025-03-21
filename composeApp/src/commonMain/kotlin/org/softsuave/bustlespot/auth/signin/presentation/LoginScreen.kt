package org.softsuave.bustlespot.auth.signin.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import bustlespot.composeapp.generated.resources.Res
import bustlespot.composeapp.generated.resources.ic_bustlespot
import bustlespot.composeapp.generated.resources.ic_password_visibility_off
import bustlespot.composeapp.generated.resources.ic_password_visible
import bustlespot.composeapp.generated.resources.loginBustleIcon
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.softsuave.bustlespot.MainViewModel
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.navigation.Home
import org.softsuave.bustlespot.auth.utils.LoadingScreen
import org.softsuave.bustlespot.auth.utils.PrimaryButton
import org.softsuave.bustlespot.auth.utils.UiEvent
import org.softsuave.bustlespot.browser.WebLinks.FORGOT_PASSWORD
import org.softsuave.bustlespot.browser.WebLinks.SIGN_UP
import org.softsuave.bustlespot.browser.openWebLink
import org.softsuave.bustlespot.utils.BustleSpotRed

@Composable
fun LoginScreen(
    navController: NavHostController
) {
    val loginViewModel = koinViewModel<LoginViewModel>()
    val mainViewModel = koinViewModel<MainViewModel>()
    val emailState = loginViewModel.email.collectAsState()
    val passwordState = loginViewModel.password.collectAsState()
    val uiEvent by loginViewModel.uiEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionManager: SessionManager = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var passwordVisibility by remember {
        mutableStateOf(false)
    }
    var isLoading by mutableStateOf(false)
    coroutineScope.launch {
        sessionManager.flowAccessToken.collectLatest { token ->
            sessionManager.updateAccessToken(token)
        }
        sessionManager.flowFirstName.collectLatest { firstName ->
            sessionManager.updateUserFirstName(firstName)
        }
        sessionManager.flowLastName.collectLatest { lastName ->
            sessionManager.updateUserLastName(lastName)
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BustleSpotRed,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                Image(
                    painter = painterResource(Res.drawable.loginBustleIcon),
                    contentDescription = "Bustlespot Logo",
                    modifier = Modifier.size(180.dp)
                )


                Box(
                    modifier = Modifier.background(
                        Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Sign In",
                            color = Color.Black,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Box(
                            modifier = Modifier.width(64.dp).height(5.dp).background(
                                BustleSpotRed,
                                RoundedCornerShape(20.dp)
                            )
                        )

                        TextField(
                            value = emailState.value.value,
                            onValueChange = { loginViewModel.onEvent(LoginEvent.EmailChanged(it)) },
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                errorIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                focusedIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            ),
                            placeholder = { Text("Email", modifier = Modifier.alpha(0.5f)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            isError = emailState.value.error.isNotEmpty(),
                            maxLines = 1,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = "Email Icon",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            supportingText = {
                                if (emailState.value.error.isNotEmpty()) {
                                    Text(
                                        text = emailState.value.error,
                                        fontFamily = androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        TextField(
                            value = passwordState.value.value,
                            onValueChange = { loginViewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground,
                                errorIndicatorColor = MaterialTheme.colorScheme.error,
                                focusedIndicatorColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            placeholder = { Text("Password", modifier = Modifier.alpha(0.5f)) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter || keyEvent.key == Key.Tab) {
                                    if (emailState.value.isValid && passwordState.value.isValid) {
                                        loginViewModel.onEvent(LoginEvent.SubmitLogin)
                                    }
                                    true // Consume event
                                } else {
                                    false
                                }
                            },
                            maxLines = 1,
                            trailingIcon = {
                                Icon(
                                    painter = if (!passwordVisibility) painterResource(Res.drawable.ic_password_visible) else painterResource(
                                        Res.drawable.ic_password_visibility_off
                                    ),
                                    contentDescription = "Toggle Password Visibility",
                                    modifier = Modifier.clickable(
                                        interactionSource = MutableInteractionSource(),
                                        indication = null
                                    ) {
                                        passwordVisibility = !passwordVisibility
                                    }.pointerHoverIcon(PointerIcon.Hand)
                                        .focusable(interactionSource = MutableInteractionSource())
                                )
                            },
                            isError = passwordState.value.error.isNotEmpty(),
                            supportingText = {
                                if (passwordState.value.error.isNotEmpty()) {
                                    Text(
                                        text = passwordState.value.error,
                                        fontFamily = androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisibility) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp).clickable(
                                    interactionSource = MutableInteractionSource(),
                                    indication = null
                                ) {
                                    // navController.navigate(AuthScreen.ForgotPassword.route)
                                    openWebLink(FORGOT_PASSWORD)
                                }.pointerHoverIcon(PointerIcon.Hand),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        PrimaryButton(
                            buttonText = "Login",
                            onClick = {
                                loginViewModel.onEvent(LoginEvent.SubmitLogin)
                            },
                            enabled = emailState.value.isValid && passwordState.value.isValid,
                            isLoading = isLoading
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Don't have an account?",
                                color = Color.Black,
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Sign Up",
                                color = BustleSpotRed,
                                modifier = Modifier.clickable(
                                    interactionSource = MutableInteractionSource(),
                                    indication = null
                                ) {
                                    //navController.navigate(AuthScreen.SignUp.route)
                                    openWebLink(SIGN_UP)
                                }.pointerHoverIcon(PointerIcon.Hand),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                    }
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Let's Start!",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "version",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            when (uiEvent) {

                is UiEvent.Failure -> {
                    rememberCoroutineScope().launch {
                        snackbarHostState.showSnackbar((uiEvent as UiEvent.Failure).error)
                    }
                    isLoading = false
                }

                UiEvent.Loading -> {
                    // LoadingScreen()
                    isLoading = true
                }

                is UiEvent.Success -> {
                    rememberCoroutineScope().launch {
                        snackbarHostState.showSnackbar("Login Successful.")
                        navController.navigate(Home.Organisation.route)
                    }
                    isLoading = false
                }

                null -> {
                    println("null")
                }
            }
        }
    }
}
