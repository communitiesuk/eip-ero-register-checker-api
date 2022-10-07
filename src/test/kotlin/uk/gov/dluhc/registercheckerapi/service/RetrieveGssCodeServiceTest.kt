package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeGeneralException
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerGeneralException
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode

@ExtendWith(MockitoExtension::class)
internal class RetrieveGssCodeServiceTest {

    @Mock
    private lateinit var ierApiClient: IerApiClient

    @Mock
    private lateinit var eroService: EroService

    @InjectMocks
    private lateinit var retrieveGssCodeService: RetrieveGssCodeService

    @Nested
    inner class GetGssCodeFromCertificateSerial {

        @Test
        fun `should throw IER not found exception given IER API client throws IER not found exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerEroNotFoundException(certificateSerial)
            given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial) },
                IerEroNotFoundException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verifyNoInteractions(eroService)
        }

        @Test
        fun `should throw general IER exception given IER API client throws general exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerGeneralException("Error getting eroId for certificate serial $certificateSerial")
            given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial) },
                IerGeneralException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verifyNoInteractions(eroService)
        }

        @Test
        fun `should throw ERO not found exception given ERO API client throws not found exception`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val expected = ElectoralRegistrationOfficeNotFoundException(certificateSerial)

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial) },
                ElectoralRegistrationOfficeNotFoundException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }

        @Test
        fun `should throw general ERO exception given ERO API client throws general exception`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val expected = ElectoralRegistrationOfficeGeneralException("Some error getting ERO $eroIdFromIerApi")

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial) },
                ElectoralRegistrationOfficeGeneralException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }

        @Test
        fun `should return empty gssCode when valid certificateSerial provided`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willReturn(emptyList())

            // When
            val actual = retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEmpty()
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }

        @Test
        fun `should return one gssCode when valid certificateSerial provided`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val expectedGssCodes = listOf(getRandomGssCode())

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willReturn(expectedGssCodes)

            // When
            val actual = retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial)

            // Then
            assertThat(actual).isEqualTo(expectedGssCodes)
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }

        @Test
        fun `should return multiple gssCodes when valid certificateSerial provided`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val gssCodeFromEroApi = getRandomGssCode()
            val anotherGssCodeFromEroApi = getRandomGssCode()
            val expectedGssCodes = listOf(gssCodeFromEroApi, anotherGssCodeFromEroApi)

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willReturn(expectedGssCodes)

            // When
            val actual = retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial)

            // Then
            assertThat(actual).isEqualTo(expectedGssCodes)
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }
    }
}
