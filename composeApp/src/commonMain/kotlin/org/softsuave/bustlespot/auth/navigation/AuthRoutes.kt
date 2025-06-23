package org.softsuave.bustlespot.auth.navigation


sealed class AuthScreen(val route: String) {
    data object SignIn : AuthScreen("login")
    data object SignUp : AuthScreen("signup")
    data object ForgotPassword : AuthScreen("forgot_password")
    data object ChangePassword : AuthScreen("change_password")
    data object LogOut : AuthScreen("logout")
}

sealed class Home(val route: String) {
    data object Organisation : Home("organisation")
    data object OrganisationModule : Home("organisation_module")
    data object Tracker : Home("tracker")
}



