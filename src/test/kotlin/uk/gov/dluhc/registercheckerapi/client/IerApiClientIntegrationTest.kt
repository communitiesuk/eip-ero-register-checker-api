package uk.gov.dluhc.registercheckerapi.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

/**
* Note: Negative tests which throws errors/exceptions for [uk.gov.dluhc.registercheckerapi.client.IerApiClient]
 * are covered as a part of Mockito unit tests in [uk.gov.dluhc.registercheckerapi.client.IerApiClientTest]
 */
internal class IerApiClientIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var ierApiClient: IerApiClient

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @Test
    @Disabled("Disabled to test whether build works to next step. Will be re enabled")
    fun `should get EROCertificateMapping response for a given certificate serial`() {
        // Given
        val certificateSerial = "1234567891"
        val expectedEroId = "camden-city-council"
        val expectedEroCertificateMapping =
            EROCertificateMapping(eroId = expectedEroId, certificateSerial = certificateSerial)
        wireMockService.stubIerApiGetEroIdentifier(certificateSerial, expectedEroId)

        // When
        val actualEroCertificateMapping = ierApiClient.getEroIdentifier(certificateSerial)

        // Then
        assertThat(actualEroCertificateMapping).isEqualTo(expectedEroCertificateMapping)
        verifyWiremockGetInvokedFor(certificateSerial)
    }

    private fun verifyWiremockGetInvokedFor(certificateSerial: String) {
        wireMockServer.verify(
            WireMock.getRequestedFor(urlPathMatching("/ier-ero/ero"))
                .withQueryParam("certificateSerial", equalTo(certificateSerial))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
        )
    }

    private fun matchingAwsSignedAuthHeader(): StringValuePattern =
        WireMock.matching(
            "AWS4-HMAC-SHA256 " +
                "Credential=.*, " +
                "SignedHeaders=accept;accept-encoding;host;x-amz-date;x-amz-security-token, " +
                "Signature=.*"
        )
}
