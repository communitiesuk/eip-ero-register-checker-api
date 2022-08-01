package uk.gov.dluhc.registercheckerapi.client

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.dluhc.external.ier.models.EROCertificateMapping

private val logger = KotlinLogging.logger {}

@Component
class IerManagementApiClient(private val ierManagementWebClient: WebClient) {

    /**
     * Calls the external `ier-management-api` to return a [EROCertificateMapping] for the specified certificate serial.
     *
     * @param certificateserial the certificate serial number for getting the Ero mapping
     * @return a [EROCertificateMapping] containing eroId and certificate serial
     * @throws [IerManagementApiException] concrete implementation if the API returns an error
     */
    fun getEroIdentifier(certificateSerial: String): EROCertificateMapping =
        ierManagementWebClient
            .get()
            .uri(buildUriString(certificateSerial))
            .retrieve()
            .bodyToMono(EROCertificateMapping::class.java)
            .onErrorResume { ex -> handleException(ex, certificateSerial) }
            .block()!!

    private fun buildUriString(certificateSerial: String): String =
        UriComponentsBuilder
            .fromUriString("/ero")
            .queryParam("certificateSerial", certificateSerial)
            .build()
            .toUriString()

    private fun handleException(ex: Throwable, certificateSerial: String): Mono<EROCertificateMapping> =
        if (ex is WebClientResponseException) {
            handleWebClientResponseException(ex, certificateSerial)
        } else {
            logger.error(ex) { "Unhandled exception thrown by WebClient" }
            Mono.error(IerGeneralException("Unhandled error getting EROCertificateMapping for certificate serial $certificateSerial"))
        }

    private fun handleWebClientResponseException(
        ex: WebClientResponseException,
        certificateSerial: String
    ): Mono<EROCertificateMapping> =
        if (ex.statusCode == HttpStatus.NOT_FOUND)
            Mono.error(IerNotFoundException(certificateSerial))
        else
            Mono.error(IerGeneralException("Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [${ex.message}]"))
}
