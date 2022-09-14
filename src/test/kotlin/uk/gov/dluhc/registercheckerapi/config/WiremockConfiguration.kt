package uk.gov.dluhc.registercheckerapi.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate

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
    fun wireMockIerRestTemplate(
        // TODO ierClientHttpRequestFactory: ClientHttpRequestFactory,
        wireMockServer: WireMockServer
    ): RestTemplate =
        RestTemplateBuilder()
            // TODO .requestFactory { ierClientHttpRequestFactory }
            .rootUri("http://localhost:${wireMockServer.port()}/ier-ero")
            .build()
}
