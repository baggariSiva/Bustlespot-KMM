package org.softsuave.bustlespot.network.models.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectsData(
    @SerialName("projects")
    val projectList: List<Project>
)