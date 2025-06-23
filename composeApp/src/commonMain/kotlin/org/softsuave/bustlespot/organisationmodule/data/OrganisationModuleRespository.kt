package org.softsuave.bustlespot.organisationmodule.data

import kotlinx.coroutines.flow.Flow
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.models.response.OrganisationModule

fun interface OrganisationModuleRepository {
    fun getOrganisationModules(
        organisationId: String
    ): Flow<Result<List<OrganisationModule>>>
}