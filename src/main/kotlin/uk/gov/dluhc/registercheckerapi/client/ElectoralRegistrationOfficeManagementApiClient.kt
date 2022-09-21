package uk.gov.dluhc.registercheckerapi.client

import mu.KotlinLogging
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse

private val logger = KotlinLogging.logger {}

/**
 * Client class for interacting with the REST API `ero-management-api`
 */
@Component
class ElectoralRegistrationOfficeManagementApiClient(private val eroManagementWebClient: WebClient) {

    /**
     * Calls the `ero-management-api` to return a [ElectoralRegistrationOfficeResponse] for the specified eroId.
     *
     * @param eroId the ID of the ERO to return
     * @return a [ElectoralRegistrationOfficeResponse] for the ERO
     * @throws [ElectoralRegistrationOfficeManagementApiException] concrete implementation if the API returns an error
     */
    fun getElectoralRegistrationOffice(eroId: String): ElectoralRegistrationOfficeResponse =
        eroManagementWebClient
            .get()
            .uri("/eros/{eroId}", eroId)
            .retrieve()
            .bodyToMono(ElectoralRegistrationOfficeResponse::class.java)
            .onErrorResume { ex -> handleException(ex, eroId) }
            .block()!!

    private fun handleException(ex: Throwable, eroId: String): Mono<ElectoralRegistrationOfficeResponse> =
        if (ex is WebClientResponseException) {
            handleWebClientResponseException(ex, eroId)
        } else {
            logger.error(ex) { "Unhandled exception thrown by WebClient" }
            Mono.error(ElectoralRegistrationOfficeGeneralException("Unhandled error getting ERO $eroId"))
        }

    private fun handleWebClientResponseException(ex: WebClientResponseException, eroId: String): Mono<ElectoralRegistrationOfficeResponse> =
        if (ex.statusCode == NOT_FOUND)
            Mono.error(ElectoralRegistrationOfficeNotFoundException(eroId))
        else
            Mono.error(ElectoralRegistrationOfficeGeneralException("Error ${ex.message} getting ERO $eroId"))
}
