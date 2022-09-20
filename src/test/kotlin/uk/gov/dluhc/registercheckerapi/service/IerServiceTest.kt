package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerGeneralException

@ExtendWith(MockitoExtension::class)
internal class IerServiceTest {

    @Mock
    private lateinit var ierApiClient: IerApiClient

    @InjectMocks
    private lateinit var ierService: IerService

    @Test
    fun `should get eroId for a given certificate serial`() {
        // Given
        val certificateSerial = "123456789"

        given(ierApiClient.getEroIdentifier(any()))
            .willReturn(EROCertificateMapping("1234", certificateSerial))

        val expectedEroId = "1234"

        // When
        val actualEroId = ierService.getEroIdentifierForCertificateSerial(certificateSerial)

        // Then
        assertThat(actualEroId).isEqualTo(expectedEroId)
        verify(ierApiClient).getEroIdentifier(certificateSerial)
    }

    @Test
    fun `should not return eroId given API client throws IER not found exception`() {
        // Given
        val certificateSerial = "123456789"

        val expected = IerEroNotFoundException(certificateSerial)
        given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

        // When
        val ex = catchThrowableOfType(
            { ierService.getEroIdentifierForCertificateSerial(certificateSerial) },
            IerEroNotFoundException::class.java
        )

        // Then
        assertThat(ex).isEqualTo(expected)
    }

    @Test
    fun `should not return eroId given API client throws general exception`() {
        // Given
        val certificateSerial = "123456789"

        val expected = IerGeneralException("Error getting eroId for certificate serial $certificateSerial")
        given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

        // When
        val ex = catchThrowableOfType(
            { ierService.getEroIdentifierForCertificateSerial(certificateSerial) },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex).isEqualTo(expected)
    }
}
