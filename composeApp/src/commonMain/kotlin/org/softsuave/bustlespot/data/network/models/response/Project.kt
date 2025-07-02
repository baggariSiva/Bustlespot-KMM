package org.softsuave.bustlespot.data.network.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
//
//@Serializable
//data class Project(
//    @SerialName("projectId")
//    val projectId: Int,
//    val name: String,
//    val status: Int,
//    val startDate: String,
//    val userId: Int? = null,
//    @SerialName("roleId")
//    val roleId: Int? = null,
//    @SerialName("users")
//    private val usersJson: String
//) : DisplayItem() {
//    val users: List<ProjectUser>?
//        get() = try {
//            Json.decodeFromString(usersJson) // Safely parse users JSON
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null // Return null if parsing fails
//        }
//}


@Serializable
data class Project(
    @SerialName("project_id")
    val projectId: String,
    @SerialName("project_manager_email")
    val projectManagerEmail: String,
    @SerialName("module_id")
    val moduleId: String,
    @SerialName("project_name")
    val name: String,
    @SerialName("project_category")
    val projectCategory: String? =  null,
    @SerialName("project_cover_photo")
    val projectCoverPhoto: String? = null,
    @SerialName("created_date")
    val createdDate: String,
    @SerialName("is_delected")
    val isDeleted: Boolean
)


open class DisplayItem {

}

@Serializable
data class ProjectUser(
    val roleId: Int? = null,
    val userId: String? = null,
    val fullName: String? = null,
    val profileImage: String? = null// Can be null
)