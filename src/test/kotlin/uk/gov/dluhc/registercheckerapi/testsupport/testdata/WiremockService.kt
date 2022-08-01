package uk.gov.dluhc.registercheckerapi.testsupport.testdata

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.springframework.stereotype.Service

@Service
class WiremockService(private val wireMockServer: WireMockServer) {

    fun resetAllStubsAndMappings() {
        wireMockServer.resetAll()
    }

    fun stubIerApiGetEroIdentifier() {
        wireMockServer.stubFor(
            get(urlPathMatching("/ier-management-api/ier-ero/.*"))
                .willReturn(
                    responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                                {
                                    "eroId": "1234",
                                    "certificateSerial": "543219999"
                                }
                            """.trimIndent()
                        )
                )
        )
    }

    fun stubIerApiGetEroIdentifierThrowsInternalServerError() {
        wireMockServer.stubFor(
            get(urlPathMatching("/ier-management-api/ier-ero/.*"))
                .willReturn(
                    responseDefinition()
                        .withStatus(500)
                )
        )
    }

    fun stubIerApiGetEroIdentifierThrowsNotFoundError() {
        wireMockServer.stubFor(
            get(urlPathMatching("/ier-management-api/ier-ero/.*"))
                .willReturn(
                    responseDefinition()
                        .withStatus(404)
                )
        )
    }
}
