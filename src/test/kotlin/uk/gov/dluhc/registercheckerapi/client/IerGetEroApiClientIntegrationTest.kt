package uk.gov.dluhc.registercheckerapi.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

internal class IerGetEroApiClientIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var ierGetEroApiClient: IerGetEroApiClient

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @Test
    fun `should get EROCertificateMapping response for a given certificate serial`() {
        // Given
        val certificateSerial = "123456789"
        val expectedEroId = "camden-city-council"
        val expectedEroCertificateMapping = EROCertificateMapping(eroId = expectedEroId, certificateSerial = certificateSerial)
        wireMockService.stubIerApiGetEroIdentifier(certificateSerial, expectedEroId)

        // When
        val actualEroCertificateMapping = ierGetEroApiClient.getEroIdentifier(certificateSerial)

        // Then
        assertThat(actualEroCertificateMapping).isEqualTo(expectedEroCertificateMapping)
        verifyWiremockGetInvokedFor(certificateSerial)
    }

    @Test
    fun `should not get EROCertificateMapping response given API returns a 404 error`() {
        // Given
        val certificateSerial = "123456789"
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError()
        val expectedException = IerNotFoundException(certificateSerial = certificateSerial)

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierGetEroApiClient.getEroIdentifier(certificateSerial) },
            IerNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verifyWiremockGetInvokedFor(certificateSerial)
    }

    @Test
    fun `should not get EROCertificateMapping response given API returns a 500 error`() {
        // Given
        val certificateSerial = "123456789"
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

        val expectedException =
            IerGeneralException(message = "Unable to retrieve EROCertificateMapping for certificate serial [123456789] due to error: [500 Server Error: [no body]]")

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierGetEroApiClient.getEroIdentifier(certificateSerial) },
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
        )
    }
}
