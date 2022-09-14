package uk.gov.dluhc.registercheckerapi.client

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.dluhc.external.ier.models.EROCertificateMapping

private val logger = KotlinLogging.logger {}

@Component
class IerGetEroApiClient(
    private val ierRestTemplate: RestTemplate
) {

    companion object {
        private const val GET_ERO_URI = "/ero"
    }

    /**
     * Calls the external `ier-api` to return a [EROCertificateMapping] for the specified certificate serial.
     *
     * @param certificateSerial the certificate serial number for getting the Ero mapping
     * @return a [EROCertificateMapping] containing eroId and certificate serial
     * @throws [IerApiException] concrete implementation if the API returns an error
     */
    fun getEroIdentifier(certificateSerial: String): EROCertificateMapping {
        logger.info("Get IER ERO for certificateSerial=[$certificateSerial]")
        try {
            return ierRestTemplate.getForEntity(
                buildUriString(certificateSerial),
                EROCertificateMapping::class.java
            ).body!!.apply {
                logger.info { "GET IER ero response for certificateSerial=[$certificateSerial] is [$this]" }
            }
        } catch (httpClientEx: HttpClientErrorException) {
            when (httpClientEx.statusCode) {
                HttpStatus.NOT_FOUND -> throw IerNotFoundException(certificateSerial).apply {
                    logger.warn { "Error: ${this.message}" }
                }

                else -> {
                    val message =
                        "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [$this]"
                    throw IerGeneralException(message).apply {
                        logger.warn { "Error: $message" }
                    }
                }
            }
        } catch (e: RestClientException) {
            val message =
                "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [${e.message}]"
            throw IerGeneralException(message).apply {
                logger.error("Error: $message")
            }
        }
    }

    private fun buildUriString(certificateSerial: String): String =
        UriComponentsBuilder
            .fromUriString(GET_ERO_URI)
            .queryParam("certificateSerial", certificateSerial)
            .build()
            .toUriString()
}
