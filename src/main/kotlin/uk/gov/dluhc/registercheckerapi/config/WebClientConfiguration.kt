package uk.gov.dluhc.registercheckerapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {

    @Bean
    fun eroManagementWebClient(@Value("\${api.ero-management.url}") eroManagementApiUrl: String): WebClient =
        WebClient.builder()
            .baseUrl(eroManagementApiUrl)
            .build()
}
