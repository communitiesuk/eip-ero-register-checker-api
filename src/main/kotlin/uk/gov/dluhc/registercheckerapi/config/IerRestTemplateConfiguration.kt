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
    @Value("\${api.ier.sts.assume.role}") private val ierStsAssumeRole: String // TODO will be used in next PR
) {

    @Bean
    fun ierRestTemplate(): RestTemplate =
        RestTemplateBuilder()
            .rootUri(ierApiBaseUrl)
            .build()
}
