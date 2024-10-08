package uk.gov.dluhc.registercheckerapi.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import uk.gov.dluhc.external.ier.models.ERODetails
import uk.gov.dluhc.external.ier.models.ErosGet200Response
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerEroDetails
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerLocalAuthorityDetails

private const val IER_EROS_GET_URL = "/ier-ero/eros"

@Service
class WiremockService(private val wireMockServer: WireMockServer) {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    fun resetAllStubsAndMappings() {
        wireMockServer.resetAll()
    }

    fun verifyIerGetErosCalledOnce() {
        verifyIerGetErosCalled(1)
    }

    fun verifyIerGetErosCalled(count: Int) {
        wireMockServer.verify(count, getRequestedFor(urlPathMatching(IER_EROS_GET_URL)))
    }

    fun verifyIerGetErosNeverCalled() {
        verifyIerGetErosCalled(0)
    }

    fun stubIerApiGetEros(certificateSerial: String, eroId: String, gssCodes: List<String>) {
        val erosResponse = ErosGet200Response(
            eros = listOf(
                buildIerEroDetails(
                    eroIdentifier = eroId,
                    activeClientCertificateSerials = listOf(certificateSerial),
                    localAuthorities = gssCodes.map { buildIerLocalAuthorityDetails(gssCode = it) },
                )
            )
        )

        wireMockServer.stubFor(
            get(urlEqualTo(IER_EROS_GET_URL))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
                .willReturn(
                    responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(erosResponse))
                )
        )
    }

    fun stubIerApiGetEros(eros: List<ERODetails>) {
        val erosResponse = ErosGet200Response(eros = eros)

        wireMockServer.stubFor(
            get(urlEqualTo(IER_EROS_GET_URL))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
                .willReturn(
                    responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(erosResponse))
                )
        )
    }

    fun stubIerApiGetEroIdentifierThrowsInternalServerError() =
        stubIerApiGetEroIdentifierThrowsException(500)

    fun stubIerApiGetEroIdentifierThrowsNotFoundError() =
        stubIerApiGetEroIdentifierThrowsException(404)

    private fun stubIerApiGetEroIdentifierThrowsException(httpStatusCode: Int) {
        wireMockServer.stubFor(
            get(urlEqualTo(IER_EROS_GET_URL))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
                .willReturn(
                    responseDefinition()
                        .withStatus(httpStatusCode)
                )
        )
    }

    private fun matchingAwsSignedAuthHeader(): StringValuePattern =
        matching(
            "AWS4-HMAC-SHA256 " +
                "Credential=.*, " +
                "SignedHeaders=accept-encoding;host;x-amz-content-sha256;x-amz-date;x-amz-security-token;x-correlation-id, " +
                "Signature=.*"
        )
}
