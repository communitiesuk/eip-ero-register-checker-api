package uk.gov.dluhc.registercheckerapi.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

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
    fun `should return not found when EROCertificateMapping for given certificate serial is not found in IER`() {
        // Given
        val certificateSerial = "1234567892"
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError(certificateSerial)
        val expectedException = IerNotFoundException(certificateSerial = certificateSerial)

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEroIdentifier(certificateSerial) },
            IerNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verifyWiremockGetInvokedFor(certificateSerial)
    }

    @Test
    fun `should return general exception when IER returns forbidden error`() {
        // Given
        val certificateSerial = "1234567895"
        wireMockService.stubIerApiGetEroIdentifierThrowsUnauthorizedError(certificateSerial)
        val expectedException =
            IerGeneralException(message = "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [403 Forbidden: [no body]]")

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEroIdentifier(certificateSerial) },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verifyWiremockGetInvokedFor(certificateSerial)
    }

    @Test
    fun `should return general exception when IER returns internal server error`() {
        // Given
        val certificateSerial = "1234567893"
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError(certificateSerial)

        val expectedException =
            IerGeneralException(message = "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [500 Server Error: [no body]]")

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEroIdentifier(certificateSerial) },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
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
