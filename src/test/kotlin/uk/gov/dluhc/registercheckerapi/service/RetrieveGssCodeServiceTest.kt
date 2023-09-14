package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.external.ier.models.ERODetails
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerGeneralException
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerEroDetails
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerLocalAuthorityDetails

@ExtendWith(MockitoExtension::class)
internal class RetrieveGssCodeServiceTest {

    @Mock
    private lateinit var ierApiClient: IerApiClient

    @InjectMocks
    private lateinit var retrieveGssCodeService: RetrieveGssCodeService

    @Nested
    inner class GetGssCodeFromCertificateSerial {
        @Test
        fun `should throw general IER exception given IER API client throws general exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerGeneralException("Error getting eroId for certificate serial $certificateSerial")
            given(ierApiClient.getEros()).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial) },
                IerGeneralException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(ierApiClient).getEros()
        }

        @Test
        fun `should throw IER not found exception given no matching certificate serial is found`() {
            // Given
            val givenCertificateSerial = "123456789"
            val certificateSerialFromIerApi = "987654321"
            val eroId = getRandomEroId()

            given(ierApiClient.getEros()).willReturn(
                listOf(
                    buildIerEroDetails(
                        eroIdentifier = eroId,
                        activeClientCertificateSerials = listOf(certificateSerialFromIerApi),
                        localAuthorities = listOf()
                    )
                )
            )

            val expected = IerEroNotFoundException(givenCertificateSerial)

            // When
            val ex = catchThrowableOfType(
                { retrieveGssCodeService.getGssCodeFromCertificateSerial(givenCertificateSerial) },
                IerEroNotFoundException::class.java
            )

            // Then
            assertThat(ex).isInstanceOf(IerEroNotFoundException::class.java)
            assertThat(ex.message).isEqualTo("EROCertificateMapping for certificateSerial=[123456789] not found")
            verify(ierApiClient).getEros()
        }

        @Test
        fun `should return empty gssCode when valid certificateSerial provided`() {
            // Given
            val certificateSerial = "123456789"
            val eroId = getRandomEroId()

            given(ierApiClient.getEros()).willReturn(
                listOf(
                    buildIerEroDetails(
                        eroIdentifier = eroId,
                        activeClientCertificateSerials = listOf(certificateSerial),
                        localAuthorities = listOf()
                    )
                )
            )

            // When
            val actual = retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEmpty()
            verify(ierApiClient).getEros()
        }

        @Test
        fun `should return one gssCode when valid certificateSerial provided`() {
            // Given
            val certificateSerial = "123456789"
            val eroId = getRandomEroId()
            val expectedGssCodes = listOf(getRandomGssCode())

            given(ierApiClient.getEros()).willReturn(
                listOf(
                    buildIerEroDetails(
                        eroIdentifier = eroId,
                        activeClientCertificateSerials = listOf(certificateSerial),
                        localAuthorities = expectedGssCodes.map {
                            buildIerLocalAuthorityDetails(gssCode = it)
                        }
                    )
                )
            )

            // When
            val actual = retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial)

            // Then
            assertThat(actual).isEqualTo(expectedGssCodes)
            verify(ierApiClient).getEros()
        }

        @Test
        fun `should return multiple gssCodes when valid certificateSerial provided`() {
            // Given
            val certificateSerial = "123456789"
            val eroId = getRandomEroId()
            val gssCodeFromEroApi = getRandomGssCode()
            val anotherGssCodeFromEroApi = getRandomGssCode()
            val expectedGssCodes = listOf(gssCodeFromEroApi, anotherGssCodeFromEroApi)

            given(ierApiClient.getEros()).willReturn(
                listOf(
                    ERODetails(
                        eroIdentifier = eroId,
                        activeClientCertificateSerials = listOf(certificateSerial),
                        localAuthorities = expectedGssCodes.map {
                            buildIerLocalAuthorityDetails(gssCode = it)
                        }
                    )
                )
            )

            // When
            val actual = retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial)

            // Then
            assertThat(actual).isEqualTo(expectedGssCodes)
            verify(ierApiClient).getEros()
        }
    }
}
