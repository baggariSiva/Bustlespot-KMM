package org.softsuave.bustlespot.organisation.data

import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.models.response.Organisation
import kotlinx.coroutines.flow.Flow

fun interface OrganisationRepository {
    fun getAllOrganisation(): Flow<Result<List<Organisation>>>
}