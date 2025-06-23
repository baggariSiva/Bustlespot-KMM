package org.softsuave.bustlespot.data.network.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Organisation(
    @SerialName("organisation_name")
    val name: String,
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("organisation_description")
    val description: String,
    @SerialName("organisation_image_url")
    val imageUrl: String,
    @SerialName("role")
    val roleId: Int,
)