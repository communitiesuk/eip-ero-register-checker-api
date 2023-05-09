package uk.gov.dluhc.registercheckerapi.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import java.time.Duration

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

    @Test
    fun `should cache and evict cache`() {
        // Given
        val certificateSerial = "1234567891"
        val expectedEroId = "camden-city-council"
        val expectedEroId2 = "camden-city-council-2"
        val expectedEroCertificateMapping =
            EROCertificateMapping(eroId = expectedEroId, certificateSerial = certificateSerial)
        val expectedEroCertificateMapping2 =
            EROCertificateMapping(eroId = expectedEroId2, certificateSerial = certificateSerial)
        wireMockService.stubIerApiGetEroIdentifier(certificateSerial, expectedEroId)

        // When
        ierApiClient.getEroIdentifier(certificateSerial)
        wireMockService.stubIerApiGetEroIdentifier(certificateSerial, expectedEroId2)
        ierApiClient.getEroIdentifier(certificateSerial)

        // Then
        verifyWiremockGetInvokedFor(certificateSerial)

        // Within the TTL, we should retrieve result with expectedEroId and afterwards, with expectedEroId2
        await.during(timeToLive.minusMillis(500)).atMost(timeToLive).untilAsserted {
            assertThat(ierApiClient.getEroIdentifier(certificateSerial)).isEqualTo(expectedEroCertificateMapping)
        }
        await.atMost(Duration.ofSeconds(1)).untilAsserted {
            assertThat(ierApiClient.getEroIdentifier(certificateSerial)).isEqualTo(expectedEroCertificateMapping2)
        }
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
                "SignedHeaders=accept;accept-encoding;host;x-amz-date;x-amz-security-token;x-correlation-id, " +
                "Signature=.*"
        )
}
