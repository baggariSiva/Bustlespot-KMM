package org.softsuave.bustlespot.organisation.data

import com.example.Database
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.softsuave.bustlespot.SessionManager
import org.softsuave.bustlespot.auth.signin.data.BaseResponse
import org.softsuave.bustlespot.auth.utils.Result
import org.softsuave.bustlespot.data.network.APIEndpoints.GETALLORGANISATIONS
import org.softsuave.bustlespot.data.network.BASEURL
import org.softsuave.bustlespot.data.network.models.response.ErrorResponse
import org.softsuave.bustlespot.data.network.models.response.Organisation

class OrganisationRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager,
    private val db: Database
) : OrganisationRepository {
    override fun getAllOrganisation(): Flow<Result<List<Organisation>>> = flow {
        emit(Result.Loading)
        try {
            val response: HttpResponse = httpClient.get("$BASEURL$GETALLORGANISATIONS") {
                contentType(ContentType.Application.Json)
                bearerAuth(sessionManager.accessToken)
            }
            if (response.status == HttpStatusCode.OK) {
                val result: BaseResponse<List<Organisation>> = response.body()
                emit(Result.Success(result.data ?: listOf<Organisation>()))
            } else {
                val res: BaseResponse<ErrorResponse> = response.body()
                emit(Result.Error("${res.message}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }
}
