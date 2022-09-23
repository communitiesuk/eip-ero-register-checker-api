package uk.gov.dluhc.registercheckerapi.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
            val eroManagementUrl = getEroManagementUrl(wireMockServerPort = port())
            TestPropertyValues.of(
                "api.ier.base.url=$ierBaseUrl",
                "api.ero-management.url=$eroManagementUrl",
            ).applyTo(applicationContext)
        }

    private fun getIerEroBaseUrl(wireMockServerPort: Int) = "http://localhost:$wireMockServerPort/ier-ero"

    private fun getEroManagementUrl(wireMockServerPort: Int) = "http://localhost:$wireMockServerPort/ero-management-api"
}
