package uk.gov.dluhc.registercheckerapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/**
 * Configuration class exposing a configured [RestTemplate] suitable for calling IER REST APIs.
 */
@Configuration
class IerRestTemplateConfiguration(
    @Value("\${api.ier.base.url}") private val ierApiBaseUrl: String,
    @Value("\${api.ier.sts.assume.role}") private val ierStsAssumeRole: String,
) {

    companion object {
        private const val API_GATEWAY_SERVICE_NAME = "execute-api"
        private const val STS_SESSION_NAME = "EROP_IER_Session"
    }

    @Bean
    fun ierRestTemplate(
        // TODO ierClientHttpRequestFactory: ClientHttpRequestFactory,
    ): RestTemplate =
        RestTemplateBuilder()
            // TODO .requestFactory { ierClientHttpRequestFactory }
            .rootUri(ierApiBaseUrl)
            .build()
}
