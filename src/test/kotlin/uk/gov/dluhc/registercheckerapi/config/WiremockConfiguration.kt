package uk.gov.dluhc.registercheckerapi.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WiremockConfiguration {

    @Bean
    fun wireMockServer(): WireMockServer =
        WireMockServer(
            options().dynamicPort()
        ).apply {
            start()
        }

    @Bean
    @Primary
    fun wireMockEroManagementWebClient(wireMockServer: WireMockServer): WebClient =
        WebClient.builder()
            .baseUrl("http://localhost:${wireMockServer.port()}/ier-management-api/ier-ero")
            .build()
}
