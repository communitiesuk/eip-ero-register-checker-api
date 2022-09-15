package uk.gov.dluhc.registercheckerapi.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate

@Configuration
class WiremockConfiguration {

    @Bean
    fun wireMockServer(applicationContext: ConfigurableApplicationContext): WireMockServer =
        WireMockServer(
            options()
                .dynamicPort()
                .dynamicHttpsPort()
        ).apply {
            start()
            val ierBaseUrl = getIerEroBaseUrl(wireMockServerPort = port())
            TestPropertyValues.of(
                "api.ier.base.url=$ierBaseUrl",
            ).applyTo(applicationContext)
        }

    @Bean
    @Primary
    fun wireMockIerRestTemplate(wireMockServer: WireMockServer): RestTemplate =
        RestTemplateBuilder()
            .rootUri(getIerEroBaseUrl(wireMockServerPort = wireMockServer.port()))
            .build()

    private fun getIerEroBaseUrl(wireMockServerPort: Int) = "http://localhost:$wireMockServerPort/ier-ero"
}
