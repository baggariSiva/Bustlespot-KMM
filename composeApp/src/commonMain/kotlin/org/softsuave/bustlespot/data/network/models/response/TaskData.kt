package org.softsuave.bustlespot.data.network.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskData(
    @SerialName("task_id")
    val taskId: String,

    @SerialName("task_name")
    val name: String,

    @SerialName("status_id")
    val statusId: String? = null,

    @SerialName("schedule_time")
    val scheduleTime: String? = null,

    @SerialName("updated_date")
    val updatedDate: String? = null,

    @SerialName("task_description")
    val taskDescription: String? = null,

    @SerialName("priority")
    val priority: String,

    @SerialName("due_date")
    val dueDate: String,

    @SerialName("created_date")
    val createdDate: String,

    @SerialName("project_id")
    val projectId: String,

    @SerialName("working_time")
    var time : Float,

    @SerialName("idle_time")
    var unTrackedTime : Float,

    @SerialName("last_screenshot_time")
    val lastScreenShotTime : String? = null,

    @SerialName("last_screenshot")
    val lastScreenshot : String? = null,
)