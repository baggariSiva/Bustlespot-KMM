package org.softsuave.bustlespot

import com.example.Database
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.contains
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.koin.core.KoinApplication.Companion.init

class SessionManager(
    private val settings: ObservableSettings,
    private val db: Database
) {
    var isLoggedIn = MutableStateFlow(false)
        private set

    var isSending = MutableStateFlow(false)
        private set

    val flowAccessToken: Flow<String> = callbackFlow {
        val listener = settings.addStringListener("access_token", "", ::trySend)
        awaitClose { listener.deactivate() }
    }

    val flowFirstName: Flow<String> = callbackFlow {
        val listener = settings.addStringListener("first_name", "", ::trySend)
        awaitClose { listener.deactivate() }
    }

    val flowLastName: Flow<String> = callbackFlow {
        val listener = settings.addStringListener("last_name", "", ::trySend)
        awaitClose { listener.deactivate() }
    }

    val flowUserId: Flow<Int> = callbackFlow {
        val listener = settings.addIntListener("user_id", 0, ::trySend)
        awaitClose { listener.deactivate() }
    }

    var accessToken: String
        get() = settings.getString("access_token", "")
        set(value) {
            settings.putString("access_token", value)
        }

    var userId: String
        get() = settings.getString("user_id", "")
        set(value) {
            settings.putString("user_id", value)
        }
    var userFirstName: String
        get() = settings.getString("first_name", "")
        set(value) {
            settings.putString("first_name", value)
        }


    var userLastName: String
        get() = settings.getString("last_name", "")
        set(value) {
            settings.putString("last_name", value)
        }

    fun updateAccessToken(token: String): Boolean {
        isLoggedIn.value = true
        accessToken = token
        println("Updated access token. isLoggedIn = ${isLoggedIn.value}")
        return true
    }

    fun updateUserFirstName(firstName: String) {
        userFirstName = firstName
    }

    fun updateUserLastName(lastName: String) {
        userLastName = lastName
    }

    fun clearSession() {
        isLoggedIn.value = false
        settings.remove("access_token")
        db.organisationDatabaseQueries.deleteAllOrganisations()
        db.activitiesDatabaseQueries.deleteAllActivities()
        db.activitiesDatabaseQueries.deleteAllFailedActivities()
        println("Session cleared. isLoggedIn = ${isLoggedIn.value}")
    }

    init {
        SettingsHelper(settings,db).clearIfFirstRun()
        isLoggedIn = MutableStateFlow(settings.getString("access_token", "").isNotEmpty())
    }
}

class SettingsHelper(private val settings: ObservableSettings,private val db:Database) {
    private val appVersionKey = "app_version"

    fun clearIfFirstRun() {
        val currentVersion = APP_VERSION
        val isFirstRun = !settings.contains(appVersionKey) || settings.getString(
            appVersionKey,
            ""
        ) != currentVersion
        if (isFirstRun) {
            // Clear all existing settings
            settings.clear()
            db.organisationDatabaseQueries.deleteAllOrganisations()
            db.activitiesDatabaseQueries.deleteAllActivities()
            db.activitiesDatabaseQueries.deleteAllFailedActivities()
            // Mark first run as completed
            settings.putString(appVersionKey, currentVersion)
        }
    }
}
