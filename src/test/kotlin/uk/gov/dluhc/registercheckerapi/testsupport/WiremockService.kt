package uk.gov.dluhc.registercheckerapi.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildLocalAuthorityResponse

private const val IER_ERO_GET_URL = "/ier-ero/.*"
private const val ERO_MANAGEMENT_ERO_GET_URL = "/ero-management-api/eros/.*"

@Service
class WiremockService(private val wireMockServer: WireMockServer) {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    fun resetAllStubsAndMappings() {
        wireMockServer.resetAll()
    }

    fun verifyIerGetEroIdentifierCalledOnce() {
        verifyIerGetEroIdentifierCalled(1)
    }

    fun verifyIerGetEroIdentifierCalled(count: Int) {
        wireMockServer.verify(count, getRequestedFor(urlPathMatching(IER_ERO_GET_URL)))
    }

    fun verifyIerGetEroIdentifierNeverCalled() {
        verifyIerGetEroIdentifierCalled(0)
    }

    fun verifyEroManagementGetEroIdentifierCalledOnce() {
        verifyEroManagementGetEroIdentifierCalled(1)
    }

    fun verifyEroManagementGetEroIdentifierCalled(count: Int) {
        wireMockServer.verify(count, getRequestedFor(urlPathMatching(ERO_MANAGEMENT_ERO_GET_URL)))
    }

    fun verifyEroManagementGetEroIdentifierNeverCalled() {
        verifyEroManagementGetEroIdentifierCalled(0)
    }

    fun stubIerApiGetEroIdentifier(certificateSerial: String, eroId: String) {
        wireMockServer.stubFor(
            get(urlEqualTo(buildGetIerEndpointUrl(certificateSerial)))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
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

    fun stubIerApiGetEroIdentifierThrowsInternalServerError(certificateSerial: String) =
        stubIerApiGetEroIdentifierThrowsException(certificateSerial, 500)

    fun stubIerApiGetEroIdentifierThrowsNotFoundError(certificateSerial: String) =
        stubIerApiGetEroIdentifierThrowsException(certificateSerial, 404)

    fun stubEroManagementGetEro(eroId: String = "1234", gssCode1: String = "E12345678", gssCode2: String = "E98765432") {
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            eroId = eroId,
            localAuthorities = mutableListOf(
                buildLocalAuthorityResponse(gssCode = gssCode1),
                buildLocalAuthorityResponse(gssCode = gssCode2)
            )
        )
        wireMockServer.stubFor(
            get(urlPathEqualTo("/ero-management-api/eros/$eroId"))
                .willReturn(
                    responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(eroResponse))
                )
        )
    }

    fun stubEroManagementGetEroThrowsNotFoundError() {
        wireMockServer.stubFor(
            get(urlPathMatching(ERO_MANAGEMENT_ERO_GET_URL))
                .willReturn(
                    responseDefinition()
                        .withStatus(404)
                )
        )
    }

    private fun stubIerApiGetEroIdentifierThrowsException(
        certificateSerial: String,
        httpStatusCode: Int
    ) {
        wireMockServer.stubFor(
            get(urlEqualTo(buildGetIerEndpointUrl(certificateSerial)))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
                .willReturn(
                    responseDefinition()
                        .withStatus(httpStatusCode)
                )
        )
    }

    private fun buildGetIerEndpointUrl(certificateSerial: String) = "/ier-ero/ero?certificateSerial=$certificateSerial"

    private fun matchingAwsSignedAuthHeader(): StringValuePattern =
        matching(
            "AWS4-HMAC-SHA256 " +
                "Credential=.*, " +
                "SignedHeaders=accept;accept-encoding;host;x-amz-date;x-amz-security-token;x-correlation-id, " +
                "Signature=.*"
        )
}
