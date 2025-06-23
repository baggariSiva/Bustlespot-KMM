package org.softsuave.bustlespot.data.network.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Project(
    @SerialName("projectId")
    val projectId: Int,
    val name: String,
    val status: Int,
    val startDate: String,
    val userId: Int? = null,
    @SerialName("roleId")
    val roleId: Int? = null,
    @SerialName("users")
    private val usersJson: String
) : DisplayItem() {
    val users: List<ProjectUser>?
        get() = try {
            Json.decodeFromString(usersJson) // Safely parse users JSON
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if parsing fails
        }
}

open class DisplayItem {

}

@Serializable
data class ProjectUser(
    val roleId: Int? = null,
    val userId: String? = null,
    val fullName: String? = null,
    val profileImage: String? = null// Can be null
)