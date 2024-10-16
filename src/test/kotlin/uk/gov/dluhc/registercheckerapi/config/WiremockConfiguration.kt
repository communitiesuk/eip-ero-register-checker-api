package uk.gov.dluhc.registercheckerapi.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

@Configuration
class WiremockConfiguration {

    @Bean
    fun wireMockServer(
        applicationContext: ConfigurableApplicationContext,
        @Value("\${logging.wiremock}") logWiremockRequests: Boolean,
    ): WireMockServer =
        WireMockServer(
            options()
                .dynamicPort()
                .dynamicHttpsPort()
        ).apply {
            if (logWiremockRequests) {
                addMockServiceRequestListener { request: Request, response: Response ->
                    val formattedHeaders = request.headers.all().joinToString("\n") {
                        "${it.key()}: ${it.values().joinToString(", ")}"
                    }
                    val logMessage = StringBuilder()
                        .appendLine("\n\nRequest sent to wiremock:")
                        .appendLine()
                        .appendLine("${request.method} ${request.absoluteUrl}")
                        .appendLine(formattedHeaders)
                        .appendLine()
                        .appendLine(request.bodyAsString)
                        .appendLine()
                        .appendLine("Response sent from wiremock:")
                        .appendLine()
                        .appendLine(response.bodyAsString)
                    logger.info { logMessage }
                }
            }
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
