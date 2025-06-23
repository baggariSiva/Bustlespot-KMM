package org.softsuave.bustlespot.tracker.data

import kotlinx.coroutines.flow.Flow
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.models.request.UpdateActivityRequest
import org.softsuave.bustlespot.data.network.models.response.GetAllActivities
import org.softsuave.bustlespot.data.network.models.response.GetAllTasks
import org.softsuave.bustlespot.data.network.models.response.Project
import org.softsuave.bustlespot.tracker.data.model.ActivityDataResponse
import org.softsuave.bustlespot.tracker.data.model.PostActivityRequest
import org.softsuave.bustlespot.tracker.ui.model.GetTasksRequest

interface TrackerRepository {
    fun getAllProjects(organisationId : String) : Flow<Result<List<Project>>>

    fun getAllTask(getTasksRequest: GetTasksRequest) : Flow<Result<GetAllTasks>>

    fun updateActivity(updateActivityRequest: UpdateActivityRequest): Flow<Result<GetAllTasks>>

    fun postUserActivity(postActivityRequest: PostActivityRequest,isRetryCalls:Boolean = false): Flow<Result<ActivityDataResponse>>

    fun getAllActivities(taskId : String) :  Flow<Result<GetAllActivities>>

    suspend fun checkLocalDbAndPostFailedActivity()

    suspend fun checkLocalDbAndPostActivity()

    fun storePostUserActivity(postActivityRequest: PostActivityRequest):Boolean
}