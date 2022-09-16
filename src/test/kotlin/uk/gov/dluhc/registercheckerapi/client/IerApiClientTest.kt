package uk.gov.dluhc.registercheckerapi.client

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import uk.gov.dluhc.external.ier.models.EROCertificateMapping

@ExtendWith(MockitoExtension::class)
internal class IerApiClientTest {

    @Mock
    private lateinit var ierRestTemplate: RestTemplate

    @InjectMocks
    private lateinit var ierApiClient: IerApiClient

    @Test
    fun `should get EROCertificateMapping response for a given certificate serial`() {
        // Given
        val certificateSerial = "1234567891"
        val expectedEroId = "camden-city-council"
        val expectedEroCertificateMapping =
            EROCertificateMapping(eroId = expectedEroId, certificateSerial = certificateSerial)
        val expectedUrl = "/ero?certificateSerial=$certificateSerial"

        given(ierRestTemplate.getForEntity(anyString(), eq(EROCertificateMapping::class.java)))
            .willReturn(ResponseEntity.ok(expectedEroCertificateMapping))

        // When
        val actualEroCertificateMapping = ierApiClient.getEroIdentifier(certificateSerial)

        // Then
        assertThat(actualEroCertificateMapping).isEqualTo(expectedEroCertificateMapping)

        verify(ierRestTemplate).getForEntity(expectedUrl, EROCertificateMapping::class.java)
    }

    @Test
    fun `should return not found when EROCertificateMapping for given certificate serial is not found in IER`() {
        // Given
        val certificateSerial = "1234567892"
        val expectedUrl = "/ero?certificateSerial=$certificateSerial"
        val expectedException = IerNotFoundException(certificateSerial = certificateSerial)

        given(ierRestTemplate.getForEntity(anyString(), eq(EROCertificateMapping::class.java)))
            .willThrow(HttpClientErrorException(HttpStatus.NOT_FOUND, "No ero found"))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEroIdentifier(certificateSerial) },
            IerNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(ierRestTemplate).getForEntity(expectedUrl, EROCertificateMapping::class.java)
    }

    @Test
    fun `should return general exception when IER returns forbidden error`() {
        // Given
        val certificateSerial = "1234567895"
        val expectedUrl = "/ero?certificateSerial=$certificateSerial"
        val exceptionMessage = "Forbidden while communicating to IER"
        val expectedException =
            IerGeneralException(message = "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [403 $exceptionMessage]")

        given(ierRestTemplate.getForEntity(anyString(), eq(EROCertificateMapping::class.java)))
            .willThrow(HttpClientErrorException(HttpStatus.FORBIDDEN, exceptionMessage))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEroIdentifier(certificateSerial) },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(ierRestTemplate).getForEntity(expectedUrl, EROCertificateMapping::class.java)
    }

    @Test
    fun `should return general exception when IER returns internal server error`() {
        // Given
        val certificateSerial = "1234567893"
        val expectedUrl = "/ero?certificateSerial=$certificateSerial"
        val expectedException =
            IerGeneralException(message = "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [500 INTERNAL_SERVER_ERROR]")

        given(ierRestTemplate.getForEntity(anyString(), eq(EROCertificateMapping::class.java)))
            .willThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEroIdentifier(certificateSerial) },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(ierRestTemplate).getForEntity(expectedUrl, EROCertificateMapping::class.java)
    }
}
