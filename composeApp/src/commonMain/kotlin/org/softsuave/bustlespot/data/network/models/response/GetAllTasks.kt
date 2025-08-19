package org.softsuave.bustlespot.data.network.models.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetAllTasks(
   @SerialName("tasks")
    val taskDetails: List<TaskData> = emptyList()
)