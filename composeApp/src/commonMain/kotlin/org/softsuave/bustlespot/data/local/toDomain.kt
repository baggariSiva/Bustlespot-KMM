package org.softsuave.bustlespot.data.local

import kotlinx.serialization.json.Json
import org.softsuave.bustlespot.data.network.models.response.Organisation
import org.softsuave.bustlespot.tracker.data.model.ActivityData

fun com.example.ActivityData.toDomain(): ActivityData {
    return ActivityData(
        taskId = this.taskId?.toString(),
        projectId = this.projectId?.toString(),
        startTime = this.startTime,
        endTime = this.endTime,
        mouseActivity = this.mouseActivity?.toInt(),
        keyboardActivity = this.keyboardActivity?.toInt(),
        totalActivity = this.totalActivity?.toInt(),
        billable = this.billable,
        notes = this.notes,
        orgId = this.organisationId?.toInt(),
        uri = this.uri,
        unTrackedTime = this.unTrackedTime
    )
}