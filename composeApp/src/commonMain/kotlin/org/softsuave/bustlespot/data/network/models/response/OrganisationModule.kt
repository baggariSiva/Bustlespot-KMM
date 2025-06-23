package org.softsuave.bustlespot.data.network.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class OrganisationModule(
    @SerialName("id")
    val moduleId: String,
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("module_name")
    val moduleName: String,
    @SerialName("module_description")
    val moduleDescription: String,
    @SerialName("module_image")
    val moduleImage: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean,
)