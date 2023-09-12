package uk.gov.dluhc.registercheckerapi.client

import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import uk.gov.dluhc.external.ier.models.ERODetails
import uk.gov.dluhc.external.ier.models.ErosGet200Response
import uk.gov.dluhc.registercheckerapi.config.IER_ELECTORAL_REGISTRATION_OFFICES_CACHE

private val logger = KotlinLogging.logger {}

@Component
class IerApiClient(
    private val ierRestTemplate: RestTemplate
) {
    companion object {
        private const val GET_EROS_URI = "/eros"
    }

    /**
     * Calls the external `ier-api` to return all EROs in an [ErosGet200Response]
     *
     * @return a list of [ERODetails]
     * @throws [IerApiException] concrete implementation if the API returns an error
     */
    @Cacheable(cacheNames = [IER_ELECTORAL_REGISTRATION_OFFICES_CACHE])
    fun getEros(): List<ERODetails> {
        logger.info { "Get EROs from IER" }
        return try {
            ierRestTemplate.getForEntity(GET_EROS_URI, ErosGet200Response::class.java).body!!.let {
                it.eros!!
            }.apply {
                logger.info { "GET IER $GET_EROS_URI response returned ${this.size} EROs" }
            }
        } catch (e: RestClientException) {
            throw handleException(e, "Error getting EROs from IER API")
        }
    }

    private fun handleException(ex: Throwable, message: String): IerApiException = if (ex is HttpClientErrorException) {
        handleHttpClientErrorException(ex, message)
    } else if (ex is HttpServerErrorException) {
        handleHttpServerErrorException(ex, message)
    } else {
        handleUnknownException(ex, message)
    }

    private fun handleHttpClientErrorException(ex: HttpClientErrorException, message: String): IerGeneralException = handleUnknownException(ex, message)

    private fun handleHttpServerErrorException(ex: HttpServerErrorException, message: String): IerGeneralException = IerGeneralException(message).also {
        logger.error(ex) { "Unexpected error from IER, status code = ${ex.statusCode}, body = ${ex.responseBodyAsString}" }
    }

    private fun handleUnknownException(ex: Throwable, message: String): IerGeneralException = IerGeneralException(message).also {
        logger.error(ex) { "Unhandled exception thrown by RestTemplate" }
    }
}
