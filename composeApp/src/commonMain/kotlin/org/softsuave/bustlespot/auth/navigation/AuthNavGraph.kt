package org.softsuave.bustlespot.auth.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import org.softsuave.bustlespot.auth.changepassword.presentation.ChangePasswordScreen
import org.softsuave.bustlespot.auth.forgotpassword.presentation.ForgotPasswordScreen
import org.softsuave.bustlespot.auth.logout.presentation.LogoutScreen
import org.softsuave.bustlespot.auth.signin.presentation.LoginScreen
import org.softsuave.bustlespot.auth.signup.presentation.SingUpScreen
import org.softsuave.bustlespot.mainnavigation.Graph
import org.softsuave.bustlespot.organisation.ui.OrganisationScreen
import org.softsuave.bustlespot.tracker.ui.TrackerScreen


fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        route = Graph.AUTHENTICATION,
        startDestination = AuthScreen.SignIn.route
    ) {
        composable(AuthScreen.SignIn.route) {
            LoginScreen(navController = navController)
        }
        composable(AuthScreen.SignUp.route) {
            SingUpScreen(navController = navController)
        }
        composable(AuthScreen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }
        composable(AuthScreen.ChangePassword.route) {
            ChangePasswordScreen(navController = navController)
        }
        composable(AuthScreen.LogOut.route) {
            LogoutScreen(navController = navController)
        }
    }
}

fun NavGraphBuilder.homeNavGraph(
    navController: NavHostController,
    onFocusReceived: () -> Unit = {}
) {
    navigation(
        route = Graph.HOME,
        startDestination = Home.Organisation.route
    ) {
        composable(Home.Organisation.route) {
            OrganisationScreen(navController = navController)
        }
        composable(
            route = "${Home.Tracker.route}/{orgId}/{orgName}",
            arguments = listOf(
                navArgument("orgName") {
                    defaultValue = "0"
                    type = NavType.StringType
                },
                navArgument("orgId") {
                    defaultValue = "0"
                    type = NavType.StringType
                }
            )
        ) { navBackStackEntry ->
            val orgId = navBackStackEntry.arguments?.getString("orgId")
            val orgName = navBackStackEntry.arguments?.getString("orgName")
            orgId?.let { it ->
                TrackerScreen(
                    navController,
                    organisationName = orgName ?: "",
                    organisationId = orgId,
                    onFocusReceived = onFocusReceived,
                )
            }

        }
    }
}