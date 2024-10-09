package uk.gov.dluhc.registercheckerapi.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerEroDetailsList
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
    fun `should get the response as a list of ERODetails`() {
        // Given
        val expectedEros = buildIerEroDetailsList()
        wireMockService.stubIerApiGetEros(expectedEros)

        // When
        val actualEros = ierApiClient.getEros()

        // Then
        assertThat(actualEros).isEqualTo(expectedEros)
        verifyWiremockGetInvoked()
    }

    @Test
    fun `should cache and evict cache`() {
        // Given
        val firstExpectedEros = buildIerEroDetailsList()
        wireMockService.stubIerApiGetEros(firstExpectedEros)

        val secondExpectedEros = buildIerEroDetailsList()

        // When
        ierApiClient.getEros()
        wireMockService.stubIerApiGetEros(secondExpectedEros)
        ierApiClient.getEros()

        // Then
        verifyWiremockGetInvoked()

        // Within the TTL, we should retrieve result with firstExpectedEros and afterwards, with secondExpectedEros
        await.during(timeToLive.minusMillis(500)).atMost(timeToLive).untilAsserted {
            assertThat(ierApiClient.getEros()).isEqualTo(firstExpectedEros)
        }
        await.atMost(Duration.ofSeconds(1)).untilAsserted {
            assertThat(ierApiClient.getEros()).isEqualTo(secondExpectedEros)
        }
    }

    private fun verifyWiremockGetInvoked() {
        wireMockServer.verify(
            WireMock.getRequestedFor(urlPathMatching("/ier-ero/eros"))
                .withHeader("Authorization", matchingAwsSignedAuthHeader())
        )
    }

    private fun matchingAwsSignedAuthHeader(): StringValuePattern =
        WireMock.matching(
            "AWS4-HMAC-SHA256 " +
                "Credential=.*, " +
                "SignedHeaders=accept-encoding;host;x-amz-content-sha256;x-amz-date;x-amz-security-token;x-correlation-id, " +
                "Signature=.*"
        )
}
