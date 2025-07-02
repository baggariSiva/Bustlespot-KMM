package org.softsuave.bustlespot.tracker.data

import com.example.Database
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.softsuave.bustlespot.Log
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.signin.data.BaseResponse
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.local.toDomain
import org.softsuave.bustlespot.data.network.APIEndpoints.GETALLACTIVITIES
import org.softsuave.bustlespot.data.network.APIEndpoints.GETALLPROJECTS
import org.softsuave.bustlespot.data.network.APIEndpoints.GETALLTASKS
import org.softsuave.bustlespot.data.network.APIEndpoints.GETORGANISATIONMODULES
import org.softsuave.bustlespot.data.network.APIEndpoints.POSTACTIVITY
import org.softsuave.bustlespot.data.network.APIEndpoints.UPDATEACTIVITY
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.data.network.models.request.UpdateActivityRequest
import org.softsuave.bustlespot.data.network.models.response.ErrorResponse
import org.softsuave.bustlespot.data.network.models.response.GetAllActivities
import org.softsuave.bustlespot.data.network.models.response.GetAllTasks
import org.softsuave.bustlespot.data.network.models.response.ModuleResponse
import org.softsuave.bustlespot.data.network.models.response.OrganisationModule
import org.softsuave.bustlespot.data.network.models.response.Project
import org.softsuave.bustlespot.tracker.data.model.ActivityData
import org.softsuave.bustlespot.tracker.data.model.ActivityDataResponse
import org.softsuave.bustlespot.tracker.data.model.PostActivityRequest

class TrackerRepositoryImpl(
    private val client: HttpClient,
    private val sessionManager: SessionManager,
    private val db: Database,
) : TrackerRepository {

    companion object {
        const val PROJECT_ID = "project_id"
        const val TASK_ID = "task_id"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val KEYBOARD_ACTIVITY = "keyboard_activity"
        const val MOUSE_ACTIVITY = "mouse_activity"
        const val TOTAL_ACTIVITY_PERCENTAGE = "total_activity_percentage"
        const val DATE = "date"
        const val IS_BILLABLE = "is_billable"
        const val IDLE_TIME = "idle_time"
        const val LAST_SCREENSHOT_TIME = "last_screenshot_time"
        const val ACTIVITY_SCREENSHOT = "activity_screenshot"
        const val LATITUDE="latitude"
        const val LONGITUDE="longitude"

        fun buildFormData(activityData: ActivityData) = formData {
            append(PROJECT_ID,  activityData.projectId ?: "")
            append(KEYBOARD_ACTIVITY, activityData.keyboardActivity.toString())
            append(MOUSE_ACTIVITY, activityData.mouseActivity.toString())
            append(TOTAL_ACTIVITY_PERCENTAGE, activityData.totalActivity.toString())
            append(START_TIME, activityData.startTime.toString())
            append(END_TIME, activityData.endTime.toString())
            append(TASK_ID, activityData.taskId.toString())
            append(IS_BILLABLE, activityData.billable.toString())

            activityData.uri?.let {
                append(ACTIVITY_SCREENSHOT, it.toString())
            }

            activityData.unTrackedTime?.let {
                append(IDLE_TIME, it.toString())
            }
            activityData.latitude?.let {
                append(LATITUDE, it.toString())
            }
            activityData.longitude?.let {
                append(LONGITUDE, it.toString())
            }

            activityData.lastScreenShotTime?.let {
                append(LAST_SCREENSHOT_TIME, it)
            }
        }
    }

    override fun getAllModules(organisationId: String): Flow<Result<List<OrganisationModule>>> =
        flow {
            emit(Result.Loading)
            try {
                val response: HttpResponse = client.get("$BASEURL$GETORGANISATIONMODULES") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(sessionManager.accessToken)
                    parameter("organisation_id", organisationId)
                }
                if (response.status == HttpStatusCode.OK) {
                    val result: BaseResponse<ModuleResponse> = response.body()
                    emit(Result.Success(result.data?.modules ?: listOf<OrganisationModule>()))
                } else {
                    val res: BaseResponse<ErrorResponse> = response.body()
                    emit(Result.Error("${res.message}"))
                }
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: "Unknown error"))
            }
        }


    override fun getAllProjects(moduleId: String): Flow<Result<List<Project>>> {
        return flow {
            try {
                emit(Result.Loading)
                val response: HttpResponse = client.get("$BASEURL$GETALLPROJECTS") {
                    contentType(ContentType.Application.Json)
                    parameter("module_id", moduleId)
                    bearerAuth(sessionManager.accessToken)
                }
                if (response.status == HttpStatusCode.OK) {
                    val data: BaseResponse<List<Project>> = response.body()
                    println(data)
                    emit(Result.Success(data.data ?: listOf<Project>()))
                } else {
                    val responseBody: BaseResponse<ErrorResponse> = response.body()
                    println(responseBody)
                    emit(Result.Error(message = "Failed to fetch Projects: ${response.status}"))
                    println("Failed to fetch Projects: ${response}")
                }
            } catch (e: Exception) {
                print(e)
                e.printStackTrace()
                emit(Result.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun getAllTask(projectId: String): Flow<Result<GetAllTasks>> {
        return flow {
            try {
                emit(Result.Loading)
                val response: HttpResponse = client.get("$BASEURL$GETALLTASKS") {
                    contentType(ContentType.Application.Json)
                    parameter("project_id", projectId)
                    bearerAuth(sessionManager.accessToken)
                }
                if (response.status == HttpStatusCode.OK) {
                    val result: BaseResponse<GetAllTasks> = response.body()
                    println(result)
                    emit(Result.Success(result.data ?: GetAllTasks(emptyList())))
                } else {
                    emit(Result.Error(message = "Failed to fetch Projects: ${response.status}"))
                }
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun updateActivity(updateActivityRequest: UpdateActivityRequest): Flow<Result<GetAllTasks>> {
        return flow {
            try {
                emit(Result.Loading)
                val response: HttpResponse = client.post("$BASEURL$UPDATEACTIVITY") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        updateActivityRequest
                    )
                    bearerAuth(sessionManager.accessToken)
                }
                if (response.status == HttpStatusCode.OK) {
                    val result: BaseResponse<GetAllTasks> = response.body()
                    println(result)
                    emit(Result.Success(result.data ?: GetAllTasks(emptyList())))
                } else {
                    println("Failed to fetch Projects: ${response.status}")
                    emit(Result.Error(message = "Failed to fetch Projects: ${response.status}"))
                }
            } catch (e: Exception) {
                println(e.message ?: "Unknown error")
                emit(Result.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun postUserActivity(
        postActivityRequest: ActivityData,
        isRetryCalls: Boolean
    ): Flow<Result<ActivityDataResponse>> {
        return flow {
            try {
                emit(Result.Loading)

                val response: HttpResponse = client.post("$BASEURL$POSTACTIVITY") {
                    contentType(ContentType.Application.Json)
                    setBody(MultiPartFormDataContent(buildFormData(activityData = postActivityRequest)))
                    bearerAuth(sessionManager.accessToken)
                }
                if (response.status == HttpStatusCode.Created) {
                    val data: ActivityDataResponse = response.body()
                    Log.d("---- activities are deleted on post success ----")
                    if (!isRetryCalls) db.activitiesDatabaseQueries.deleteAllActivities()
                    emit(Result.Success(data))
                } else {
                    Log.d("postActivity --> failed ${response.status}")
                    if (!isRetryCalls) {
//                        saveFailedPostUserActivity(postActivityRequest)
                        db.activitiesDatabaseQueries.deleteAllActivities()
                    }
                    Log.d("---- activities are deleted on post failure ---- ${response.status} -- ${response.toString()} ")
                    emit(Result.Error(message = "Failed post activity: ${response.status}"))
                }
            } catch (e: Exception) {
                Log.d("postActivity --> failed")
                if (!isRetryCalls) {
//                    saveFailedPostUserActivity(postActivityRequest)
                    db.activitiesDatabaseQueries.deleteAllActivities()
                }
                Log.d("---- activities are deleted on post failure ---- ${e.message}")
                e.printStackTrace()
                emit(Result.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun getAllActivities(taskId: String): Flow<Result<GetAllActivities>> {
        return flow {
            try {
                emit(Result.Loading)
                val response: HttpResponse = client.post("$BASEURL$GETALLACTIVITIES/$taskId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(sessionManager.accessToken)
                }
                if (response.status == HttpStatusCode.OK) {
                    val data: GetAllActivities = response.body()
                    emit(Result.Success(data))
                } else {
                    emit(Result.Error(message = "Failed to fetch Projects: ${response.status}"))
                }
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun saveFailedPostUserActivity(postActivityRequest: PostActivityRequest) {
        postActivityRequest.activityData.forEach { activityData ->
            db.transaction {
                db.activitiesDatabaseQueries.insertActivity(
                    taskId = activityData.taskId?.toLong(),
                    projectId = activityData.projectId?.toLong(),
                    startTime = activityData.startTime,
                    endTime = activityData.endTime,
                    mouseActivity = activityData.mouseActivity?.toLong(),
                    keyboardActivity = activityData.keyboardActivity?.toLong(),
                    totalActivity = activityData.totalActivity?.toLong(),
                    billable = activityData.billable,
                    notes = activityData.notes,
                    organisationId = activityData.orgId?.toLong(),
                    uri = activityData.uri,
                    unTrackedTime = activityData.unTrackedTime,
                    isFailed = 1
                )
            }
        }
        Log.d("call saved to db")
    }

    override suspend fun checkLocalDbAndPostFailedActivity() {
        Log.d("post local failed call is started")
        val localData = db.activitiesDatabaseQueries.getAllFailedActivities().executeAsList()
            .map { it.toDomain() }
        if (localData.isNotEmpty()) {
            val postActivityRequest = PostActivityRequest(localData.toMutableList())
//            postUserActivity(postActivityRequest, true).collect { result ->
//                when (result) {
//                    is Result.Error -> {
//                        sessionManager.isSending.update { false }
//                        Log.d("failed to post from local db")
//                    }
//
//                    is Result.Loading -> {
//                        sessionManager.isSending.update { true }
//                    }
//
//                    is Result.Success -> {
//                        sessionManager.isSending.update { false }
//                        Log.d("Success to post failed call from local db")
//                        db.activitiesDatabaseQueries.deleteAllFailedActivities()
//                    }
//                }
//            }
        }
    }

    override fun storePostUserActivity(postActivityRequest: PostActivityRequest): Boolean {
        try {
            postActivityRequest.activityData.forEach { activityData ->
                db.transaction {
                    db.activitiesDatabaseQueries.insertActivity(
                        taskId = activityData.taskId?.toLong(),
                        projectId = activityData.projectId?.toLong(),
                        startTime = activityData.startTime,
                        endTime = activityData.endTime,
                        mouseActivity = activityData.mouseActivity?.toLong(),
                        keyboardActivity = activityData.keyboardActivity?.toLong(),
                        totalActivity = activityData.totalActivity?.toLong(),
                        billable = activityData.billable,
                        notes = activityData.notes,
                        organisationId = activityData.orgId?.toLong(),
                        uri = activityData.uri,
                        unTrackedTime = activityData.unTrackedTime,
                        isFailed = 0
                    )
                }
            }
            Log.d("success to save to db")
            return true
        } catch (e: Exception) {
            Log.e("failed to save to db ${e.message}")
            return false
        }
    }

    override suspend fun checkLocalDbAndPostActivity() {
        Log.d("post local call is started")
        try {
            val localData = db.activitiesDatabaseQueries.getAllActivities().executeAsList()
                .map { it.toDomain() }
            if (localData.isNotEmpty()) {
                Log.d("local data is not empty size:- ${localData.size}")
                Log.d("local data is not empty size:- ${localData.toMutableList()}")
                val postActivityRequest = PostActivityRequest(localData.toMutableList())
//                postUserActivity(postActivityRequest, true).collect { result ->
//                    when (result) {
//                        is Result.Error -> {
//                            Log.d("failed to post from local db")
//                            sessionManager.isSending.update { false }
//                        }
//
//                        is Result.Loading -> {
//                            sessionManager.isSending.update { true }
//                        }
//
//                        is Result.Success -> {
//                            sessionManager.isSending.update { false }
//                            Log.d("Success to post from local db")
//                            db.activitiesDatabaseQueries.deleteAllActivities()
//                        }
//                    }
//                }
            }
        } catch (e: Exception) {
            Log.d("error in post local ${e.message}")
        }

    }
}