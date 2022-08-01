package uk.gov.dluhc.registercheckerapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {

    @Bean
    fun ierEroWebClient(@Value("\${api.ier-management.url}") ierEroApiUrl: String): WebClient =
        WebClient.builder()
            .baseUrl(ierEroApiUrl)
            // any other headers/auth headers etc we might need re: service to service communication
            .build()
}
