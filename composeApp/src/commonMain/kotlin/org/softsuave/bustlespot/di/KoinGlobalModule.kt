package org.softsuave.bustlespot.di

import com.example.Database
import com.russhwolf.settings.ObservableSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.softsuave.bustlespot.MainViewModel
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.signin.data.AccessTokenResponse
import org.softsuave.bustlespot.auth.signin.data.BaseResponse
import org.softsuave.bustlespot.createSettings
import org.softsuave.bustlespot.data.local.createDriver
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.getEngine
import org.softsuave.bustlespot.network.NetworkMonitor
import org.softsuave.bustlespot.network.NetworkMonitorProvider
import org.softsuave.bustlespot.tracker.di.trackerDiModule

val koinGlobalModule = module {
    single { MainViewModel(get()) { provideUnauthenticatedHttpClient() } }
    factory { provideHttpClient(get(), get()) }
    trackerDiModule
    single<ObservableSettings> {
        createSettings()
    }
    single<Database> {
        provideSqlDelightDatabase()
    }
    single<NetworkMonitor> {
        provideNetworkMonitorInstance()
    }
}

fun provideUnauthenticatedHttpClient(): HttpClient {
    return HttpClient(getEngine()) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

fun provideHttpClient(settings: ObservableSettings, sessionManager: SessionManager): HttpClient {
    val refreshClient = provideUnauthenticatedHttpClient()
    return HttpClient(getEngine()) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpCallValidator) {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    println("Received 401: Attempting to refresh token")
                    // Do not clear session here; let refreshTokens handle it
                }
            }
        }
        if (settings.getString("access_token", "").isNotEmpty()) {
            install(Auth) {
                bearer {
                    refreshTokens {
                        try {
                            val refreshToken = settings.getString("refresh_token", "")
                            if (refreshToken.isEmpty()) {
                                println("No refresh token available")
                                sessionManager.clearSession()
                                return@refreshTokens null
                            }
                            val response: HttpResponse =
                                refreshClient.get("$BASEURL/auth/refresh-token") {
                                    bearerAuth(refreshToken)
                                }
                            if (response.status == HttpStatusCode.OK) {
                                val newTokens = response.body<BaseResponse<AccessTokenResponse>>()
                                settings.putString(
                                    "access_token",
                                    newTokens.data?.access_token ?: ""
                                )
                                settings.putString(
                                    "refresh_token",
                                    newTokens.data?.refresh_token ?: refreshToken
                                )
                                sessionManager.updateAccessToken(newTokens.data?.access_token ?: "")
                                return@refreshTokens BearerTokens(
                                    accessToken = newTokens.data?.access_token ?: "",
                                    refreshToken = newTokens.data?.access_token ?: refreshToken
                                )
                            } else {
                                println("Refresh token API failed with status: ${response.status}")
                                sessionManager.clearSession()
                                return@refreshTokens null
                            }
                        } catch (e: Exception) {
                            println("Token refresh failed: ${e.message}")
                            sessionManager.clearSession()
                            return@refreshTokens null
                        }
                    }
                }
            }
        }
    }
}

fun provideSqlDelightDatabase(): Database {
    val driver = createDriver()
    // add migrate if needed
//    migrateDB(driver)
    return Database.invoke(driver)
}


fun provideNetworkMonitorInstance(): NetworkMonitor {
    return NetworkMonitorProvider.getInstance()
}

//fun provideRealmeDatabase(): Realm {
//    val config = RealmConfiguration.Builder(
//        schema = setOf(
//            OrganisationObj::class
//        )
//    ).compactOnLaunch()
//        .build()
//    return Realm.open(config)
//}