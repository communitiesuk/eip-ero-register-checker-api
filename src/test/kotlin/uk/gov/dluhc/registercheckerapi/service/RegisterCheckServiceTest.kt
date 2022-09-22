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
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.external.ier.models.EROCertificateMapping
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeGeneralException
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerGeneralException
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.util.UUID

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckServiceTest {

    @Mock
    private lateinit var ierApiClient: IerApiClient

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var registerCheckRepository: RegisterCheckRepository

    @Mock
    private lateinit var pendingRegisterCheckMapper: PendingRegisterCheckMapper

    @InjectMocks
    private lateinit var registerCheckService: RegisterCheckService

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
    }

    @Nested
    inner class SaveRegisterChecks {
        @Test
        fun `should save a PendingRegisterCheckDto`() {
            // Given
            val pendingRegisterCheckDto = buildPendingRegisterCheckDto()
            val registerCheck = buildRegisterCheck()
            given(pendingRegisterCheckMapper.pendingRegisterCheckDtoToRegisterCheckEntity(any())).willReturn(registerCheck)

            // When
            registerCheckService.save(pendingRegisterCheckDto)

            // Then
            verify(pendingRegisterCheckMapper).pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)
            verify(registerCheckRepository).save(registerCheck)
        }
    }

    @Nested
    inner class GetPendingRegisterChecks {

        @Test
        fun `should get empty pending register check records for a given certificate serial given ier and ero returns valid values`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val gssCodeFromEroApi = getRandomGssCode()

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willReturn(listOf(gssCodeFromEroApi))
            given(registerCheckRepository.findPendingEntriesByGssCode(any(), any())).willReturn(emptyList())

            // When
            val actualPendingRegisterChecks = registerCheckService.getPendingRegisterChecks(certificateSerial)

            // Then
            assertThat(actualPendingRegisterChecks).isEmpty()
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
            verify(registerCheckRepository).findPendingEntriesByGssCode(listOf(gssCodeFromEroApi), DEFAULT_PAGE_SIZE)
            verifyNoInteractions(pendingRegisterCheckMapper)
        }

        @Test
        fun `should get one pending register check record for a given certificate serial given ier and ero returns valid values`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val gssCodeFromEroApi = getRandomGssCode()
            val correlationId = UUID.randomUUID()
            val expectedRecordCount = 1

            val matchedRegisterCheckEntity = buildRegisterCheck(correlationId = correlationId, gssCode = gssCodeFromEroApi, status = PENDING)
            val expectedRegisterCheckDto = buildPendingRegisterCheckDto(correlationId = correlationId, gssCode = gssCodeFromEroApi)

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willReturn(listOf(gssCodeFromEroApi))
            given(registerCheckRepository.findPendingEntriesByGssCode(any(), any())).willReturn(listOf(matchedRegisterCheckEntity))
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(any())).willReturn(expectedRegisterCheckDto)

            val expectedPendingRegisterChecks = listOf(expectedRegisterCheckDto)

            // When
            val actualPendingRegisterChecks = registerCheckService.getPendingRegisterChecks(certificateSerial)

            // Then
            assertThat(actualPendingRegisterChecks).hasSize(expectedRecordCount)
            assertThat(actualPendingRegisterChecks)
                .isEqualTo(expectedPendingRegisterChecks)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
            verify(registerCheckRepository).findPendingEntriesByGssCode(listOf(gssCodeFromEroApi), DEFAULT_PAGE_SIZE)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(matchedRegisterCheckEntity)
            verifyNoMoreInteractions(pendingRegisterCheckMapper)
        }

        @Test
        fun `should get multiple pending register check records for a given certificate serial`() {
            // Given
            val certificateSerial = "123456789"
            val eroIdFromIerApi = getRandomEroId()
            val firstGssCodeFromEroApi = "E1234561"
            val secondGssCodeFromEroApi = "E567892"
            val anotherGssCodeFromEroApi = "E9876543" // No records will be matched for this gssCode
            val expectedRecordCount = 3

            val correlationId1 = UUID.fromString("74f66386-a86e-4dbc-af52-3327834f33dc")
            val correlationId2 = UUID.fromString("593dbc1d-81df-4db3-8ed6-64f05c083376")
            val correlationId3 = UUID.fromString("99147305-ba6d-4c14-8609-5d777afe4dc3")

            val firstRegisterCheckEntity = buildRegisterCheck(correlationId = correlationId1, gssCode = firstGssCodeFromEroApi, status = PENDING)
            val secondRegisterCheckEntity = buildRegisterCheck(correlationId = correlationId2, gssCode = firstGssCodeFromEroApi, status = PENDING)
            val thirdRegisterCheckEntity = buildRegisterCheck(correlationId = correlationId3, gssCode = secondGssCodeFromEroApi, status = PENDING)

            val firstRegisterCheckDto = buildPendingRegisterCheckDto(correlationId = correlationId1, gssCode = firstGssCodeFromEroApi)
            val secondRegisterCheckDto = buildPendingRegisterCheckDto(correlationId = correlationId2, gssCode = firstGssCodeFromEroApi)
            val thirdRegisterCheckDto = buildPendingRegisterCheckDto(correlationId = correlationId3, gssCode = secondGssCodeFromEroApi)

            given(ierApiClient.getEroIdentifier(any())).willReturn(EROCertificateMapping(eroId = eroIdFromIerApi, certificateSerial = certificateSerial))
            given(eroService.lookupGssCodesForEro(any())).willReturn(listOf(firstGssCodeFromEroApi, secondGssCodeFromEroApi, anotherGssCodeFromEroApi))
            given(registerCheckRepository.findPendingEntriesByGssCode(any(), any())).willReturn(listOf(firstRegisterCheckEntity, secondRegisterCheckEntity, thirdRegisterCheckEntity))
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(eq(firstRegisterCheckEntity))).willReturn(firstRegisterCheckDto)
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(eq(secondRegisterCheckEntity))).willReturn(secondRegisterCheckDto)
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(eq(thirdRegisterCheckEntity))).willReturn(thirdRegisterCheckDto)

            val expectedPendingRegisterChecks = listOf(firstRegisterCheckDto, secondRegisterCheckDto, thirdRegisterCheckDto)

            // When
            val actualPendingRegisterChecks = registerCheckService.getPendingRegisterChecks(certificateSerial)

            // Then
            assertThat(actualPendingRegisterChecks).hasSize(expectedRecordCount)
            assertThat(actualPendingRegisterChecks)
                .isEqualTo(expectedPendingRegisterChecks)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
            verify(ierApiClient).getEroIdentifier(certificateSerial)
            verify(eroService).lookupGssCodesForEro(eroIdFromIerApi)
            verify(registerCheckRepository).findPendingEntriesByGssCode(listOf(firstGssCodeFromEroApi, secondGssCodeFromEroApi, anotherGssCodeFromEroApi), DEFAULT_PAGE_SIZE)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(firstRegisterCheckEntity)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(secondRegisterCheckEntity)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(thirdRegisterCheckEntity)
            verifyNoMoreInteractions(pendingRegisterCheckMapper)
        }

        @Test
        fun `should throw IER not found exception given IER API client throws IER not found exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerEroNotFoundException(certificateSerial)
            given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { registerCheckService.getPendingRegisterChecks(certificateSerial) },
                IerEroNotFoundException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
        }

        @Test
        fun `should throw general IER exception given IER API client throws general exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerGeneralException("Error getting eroId for certificate serial $certificateSerial")
            given(ierApiClient.getEroIdentifier(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(
                { registerCheckService.getPendingRegisterChecks(certificateSerial) },
                IerGeneralException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
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
                { registerCheckService.getPendingRegisterChecks(certificateSerial) },
                ElectoralRegistrationOfficeNotFoundException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
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
                { registerCheckService.getPendingRegisterChecks(certificateSerial) },
                ElectoralRegistrationOfficeGeneralException::class.java
            )

            // Then
            assertThat(ex).isEqualTo(expected)
        }
    }
}
