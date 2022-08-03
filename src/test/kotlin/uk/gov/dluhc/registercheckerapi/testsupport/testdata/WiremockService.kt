package uk.gov.dluhc.registercheckerapi.testsupport.testdata

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

private const val IER_ERO_GET_URL = "/ier-ero/.*"

@Service
class WiremockService(private val wireMockServer: WireMockServer) {

    fun resetAllStubsAndMappings() {
        wireMockServer.resetAll()
    }

    fun verifyGetEroIdentifierCalledOnce() {
        verifyGetEroIdentifierCalled(1)
    }

    fun verifyGetEroIdentifierCalled(count: Int) {
        wireMockServer.verify(count, getRequestedFor(urlPathMatching(IER_ERO_GET_URL)))
    }

    fun stubIerApiGetEroIdentifier(certificateSerial: String, eroId: String) {
        wireMockServer.stubFor(
            get(urlPathMatching(IER_ERO_GET_URL))
                .willReturn(
                    responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """
                                {
                                    "eroId": "$eroId",
                                    "certificateSerial": "$certificateSerial"
                                }
                            """.trimIndent()
                        )
                )
        )
    }

    fun stubIerApiGetEroIdentifier(certificateSerial: String) {
        stubIerApiGetEroIdentifier(certificateSerial, "1234")
    }

    fun stubIerApiGetEroIdentifierThrowsInternalServerError() {
        wireMockServer.stubFor(
            get(urlPathMatching(IER_ERO_GET_URL))
                .willReturn(
                    responseDefinition()
                        .withStatus(500)
                )
        )
    }

    fun stubIerApiGetEroIdentifierThrowsNotFoundError() {
        wireMockServer.stubFor(
            get(urlPathMatching(IER_ERO_GET_URL))
                .willReturn(
                    responseDefinition()
                        .withStatus(404)
                )
        )
    }
}
