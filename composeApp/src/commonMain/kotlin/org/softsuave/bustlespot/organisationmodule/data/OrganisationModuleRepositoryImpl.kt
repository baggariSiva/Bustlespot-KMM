package org.softsuave.bustlespot.organisationmodule.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.signin.data.BaseResponse
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.APIEndpoints.GETORGANISATIONMODULES
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.data.network.models.response.ErrorResponse
import org.softsuave.bustlespot.data.network.models.response.Organisation
import org.softsuave.bustlespot.data.network.models.response.OrganisationModule

class OrganisationModuleRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager,
): OrganisationModuleRepository {
    override fun getOrganisationModules(
        organisationId: String
    ): Flow<Result<List<OrganisationModule>>> = flow {
        emit(Result.Loading)
        try {
            val response: HttpResponse = httpClient.get("$BASEURL$GETORGANISATIONMODULES") {
                contentType(ContentType.Application.Json)
                bearerAuth(sessionManager.accessToken)
                parameter("organisationId", organisationId)
            }
            if (response.status == HttpStatusCode.OK) {
                val result: BaseResponse<List<OrganisationModule>> = response.body()
                emit(Result.Success(result.data ?: listOf<OrganisationModule>()))
            } else {
                val res: BaseResponse<ErrorResponse> = response.body()
                emit(Result.Error("${res.message}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }
}