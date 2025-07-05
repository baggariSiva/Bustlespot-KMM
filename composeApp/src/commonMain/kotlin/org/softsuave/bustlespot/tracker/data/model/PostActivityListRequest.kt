package org.softsuave.bustlespot.tracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostActivityRequest(
    @SerialName("activityData") var activityData: MutableList<ActivityData> = mutableListOf()
)


@Serializable
data class ActivityData(
    @SerialName("task_id") var taskId: String? = "",
    @SerialName("project_id") var projectId: String? = "",
    @SerialName("start_time") var startTime: String? = null,
    @SerialName("end_time") var endTime: String? = null,
    @SerialName("last_screenshot_time") var lastScreenShotTime: String? = null,
    @SerialName("mouse_activity") var mouseActivity: Int? = 0,
    @SerialName("keyboard_activity") var keyboardActivity: Int? = 0,
    @SerialName("total_activity_percentage") var totalActivity: Int? = 0,
    @SerialName("is_billable") var billable: String? = "",
    @SerialName("notes") var notes: String? = null,
    @SerialName("organisationId") var orgId: Int? = 0,
    @SerialName("activity_screenshot") var uri: List<String?> = emptyList<String>(),
    @SerialName("idle_time") var unTrackedTime: Long? = null,
    var longitude: Double?= null,
    var latitude: Double?= null
)