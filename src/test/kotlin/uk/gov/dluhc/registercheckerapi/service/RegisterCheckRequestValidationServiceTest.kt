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
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckMatchCountMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckResultDto
import java.util.UUID.randomUUID

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckRequestValidationServiceTest {

    @Mock
    private lateinit var ierApiClient: IerApiClient

    @Mock
    private lateinit var eroService: EroService

    @InjectMocks
    private lateinit var registerCheckRequestValidationService: RegisterCheckRequestValidationService

    @Nested
    inner class ValidateRequestBody {

        @Test
        fun `should throw RequestMismatchException when requestId in query param mismatches requestId in payload`() {
            // Given
            val requestId = randomUUID()
            val correlationId = randomUUID()
            val registerCheckResultDto =
                buildRegisterCheckResultDto(requestId = requestId, correlationId = correlationId)
            val expected = RequestIdMismatchException(requestId, correlationId)

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody("123456789", registerCheckResultDto) },
                RequestIdMismatchException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request requestId:[$requestId] does not match with requestid:[$correlationId] in body payload")
            verifyNoInteractions(ierApiClient, eroService)
        }

        @Test
        fun `should throw RegisterCheckMatchCountMismatchException when matchCount is less than 10 and mismatches with registerCheckMatch list size in payload`() {
            // Given
            val requestId = randomUUID()
            val matchCount = 2
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = listOf(buildRegisterCheckMatchDto())
            )
            val expected = RegisterCheckMatchCountMismatchException("Request [registerCheckMatches:1] array size must be same as [registerCheckMatchCount:$matchCount] in body payload")

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody("123456789", registerCheckResultDto) },
                RegisterCheckMatchCountMismatchException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request [registerCheckMatches:1] array size must be same as [registerCheckMatchCount:2] in body payload")
            verifyNoInteractions(ierApiClient, eroService)
        }

        @Test
        fun `should throw RegisterCheckMatchCountMismatchException when matchCount is greater than 10 and registerCheckMatch is not empty`() {
            // Given
            val requestId = randomUUID()
            val matchCount = 11
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = listOf(buildRegisterCheckMatchDto())
            )
            val expected = RegisterCheckMatchCountMismatchException("Request [registerCheckMatches] array must be null or empty for [registerCheckMatchCount:$matchCount] in body payload")

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody("123456789", registerCheckResultDto) },
                RegisterCheckMatchCountMismatchException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request [registerCheckMatches] array must be null or empty for [registerCheckMatchCount:11] in body payload")
            verifyNoInteractions(ierApiClient, eroService)
        }

        @Test
        fun `should throw GssCodeMismatchException when gssCode from IER mismatches gssCode in payload`() {
            // Given
            val certificateSerial = "123456789"
            val requestId = randomUUID()
            val requestGssCode = "E12345678"
            val eroIdFromIerApi = getRandomEroId()
            val differentGssCodeFromEroApi = getRandomGssCode()
            val registerCheckResultDto = buildRegisterCheckResultDto(requestId = requestId, correlationId = requestId, gssCode = requestGssCode)
            val expected = GssCodeMismatchException(certificateSerial, requestGssCode)

            given(ierApiClient.getEroIdentifier(any())).willReturn(
                EROCertificateMapping(
                    eroId = eroIdFromIerApi,
                    certificateSerial = certificateSerial
                )
            )
            given(eroService.lookupGssCodesForEro(any())).willReturn(listOf(differentGssCodeFromEroApi))

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody(certificateSerial, registerCheckResultDto) },
                GssCodeMismatchException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request gssCode:[E12345678] does not match with gssCode for certificateSerial:[123456789]")
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }

        @Test
        fun `should throw IER not found exception given IER API client throws IER not found exception`() {
            // Given
            val certificateSerial = "123456789"
            val registerCheckResultDto = buildRegisterCheckResultDto()

            val expected = IerEroNotFoundException(certificateSerial)
            given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody(certificateSerial, registerCheckResultDto) },
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
            val registerCheckResultDto = buildRegisterCheckResultDto()
            val expected = IerGeneralException("Error getting eroId for certificate serial 123456789")

            given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody(certificateSerial, registerCheckResultDto) },
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
            val registerCheckResultDto = buildRegisterCheckResultDto()
            val expected = ElectoralRegistrationOfficeNotFoundException(certificateSerial)

            given(ierApiClient.getEroIdentifier(any())).willReturn(
                EROCertificateMapping(
                    eroId = eroIdFromIerApi,
                    certificateSerial = certificateSerial
                )
            )
            given(eroService.lookupGssCodesForEro(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody(certificateSerial, registerCheckResultDto) },
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
            val eroIdFromIerApi = "camden-city-council"
            val registerCheckResultDto = buildRegisterCheckResultDto()
            val expected = ElectoralRegistrationOfficeGeneralException("Some error getting ERO camden-city-council")

            given(ierApiClient.getEroIdentifier(any())).willReturn(
                EROCertificateMapping(
                    eroId = eroIdFromIerApi,
                    certificateSerial = certificateSerial
                )
            )
            given(eroService.lookupGssCodesForEro(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { registerCheckRequestValidationService.validateRequestBody(certificateSerial, registerCheckResultDto) },
                ElectoralRegistrationOfficeGeneralException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
        }
    }
}
