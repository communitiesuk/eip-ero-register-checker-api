package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.client.EroIdNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerGeneralException
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.EXACT_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.PendingRegisterCheckNotFoundException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckUnexpectedStatusException
import uk.gov.dluhc.registercheckerapi.mapper.AdminPendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.RegisterCheckResultMapper
import uk.gov.dluhc.registercheckerapi.messaging.MessageQueueResolver
import uk.gov.dluhc.registercheckerapi.messaging.mapper.RegisterCheckResultMessageMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildAdminPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildVcaRegisterCheckMatchFromMatchDto
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.UUID.randomUUID
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as SourceTypeEntity

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckServiceTest {

    @Mock
    private lateinit var retrieveGssCodeService: RetrieveGssCodeService

    @Mock
    private lateinit var registerCheckRepository: RegisterCheckRepository

    @Mock
    private lateinit var registerCheckResultDataRepository: RegisterCheckResultDataRepository

    @Mock
    private lateinit var pendingRegisterCheckMapper: PendingRegisterCheckMapper

    @Mock
    private lateinit var adminPendingRegisterCheckMapper: AdminPendingRegisterCheckMapper

    @Mock
    private lateinit var registerCheckResultMapper: RegisterCheckResultMapper

    @Mock
    private lateinit var registerCheckResultMessageMapper: RegisterCheckResultMessageMapper

    @Mock
    private lateinit var confirmRegisterCheckResultMessageQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var messageQueueResolver: MessageQueueResolver

    @Mock
    private lateinit var matchStatusResolver: MatchStatusResolver

    @InjectMocks
    private lateinit var registerCheckService: RegisterCheckService

    @Captor
    private lateinit var registerCheckRequestDataCaptor: ArgumentCaptor<RegisterCheckResultData>

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
            val gssCodeFromEroApi = getRandomGssCode()

            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(listOf(gssCodeFromEroApi))
            given(registerCheckRepository.findPendingEntriesByGssCodes(any(), any())).willReturn(emptyList())

            // When
            val actualPendingRegisterChecks = registerCheckService.getPendingRegisterChecks(certificateSerial, DEFAULT_PAGE_SIZE)

            // Then
            assertThat(actualPendingRegisterChecks).isNotNull
            assertThat(actualPendingRegisterChecks).isEmpty()
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verify(registerCheckRepository).findPendingEntriesByGssCodes(listOf(gssCodeFromEroApi), DEFAULT_PAGE_SIZE)
            verifyNoInteractions(pendingRegisterCheckMapper)
        }

        @Test
        fun `should get one pending register check record for a given certificate serial given ier and ero returns valid values`() {
            // Given
            val certificateSerial = "123456789"
            val gssCodeFromEroApi = getRandomGssCode()
            val correlationId = UUID.fromString("74f66386-a86e-4dbc-af52-3327834f33dc")
            val expectedRecordCount = 1

            val matchedRegisterCheckEntity = buildRegisterCheck(correlationId = correlationId, gssCode = gssCodeFromEroApi, status = PENDING)
            val expectedRegisterCheckDto = buildPendingRegisterCheckDto(correlationId = correlationId, gssCode = gssCodeFromEroApi)

            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(listOf(gssCodeFromEroApi))
            given(registerCheckRepository.findPendingEntriesByGssCodes(any(), any())).willReturn(listOf(matchedRegisterCheckEntity))
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(any())).willReturn(expectedRegisterCheckDto)

            val expectedPendingRegisterChecks = listOf(expectedRegisterCheckDto)

            // When
            val actualPendingRegisterChecks = registerCheckService.getPendingRegisterChecks(certificateSerial, DEFAULT_PAGE_SIZE)

            // Then
            assertThat(actualPendingRegisterChecks).hasSize(expectedRecordCount)
            assertThat(actualPendingRegisterChecks)
                .isEqualTo(expectedPendingRegisterChecks)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verify(registerCheckRepository).findPendingEntriesByGssCodes(listOf(gssCodeFromEroApi), DEFAULT_PAGE_SIZE)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(matchedRegisterCheckEntity)
            verifyNoMoreInteractions(registerCheckRepository, pendingRegisterCheckMapper)
        }

        @Test
        fun `should get multiple pending register check records for a given certificate serial`() {
            // Given
            val certificateSerial = "123456789"
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

            val expectedGssCodes = listOf(firstGssCodeFromEroApi, secondGssCodeFromEroApi, anotherGssCodeFromEroApi)
            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(expectedGssCodes)
            given(registerCheckRepository.findPendingEntriesByGssCodes(any(), any())).willReturn(listOf(firstRegisterCheckEntity, secondRegisterCheckEntity, thirdRegisterCheckEntity))
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(eq(firstRegisterCheckEntity))).willReturn(firstRegisterCheckDto)
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(eq(secondRegisterCheckEntity))).willReturn(secondRegisterCheckDto)
            given(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(eq(thirdRegisterCheckEntity))).willReturn(thirdRegisterCheckDto)

            val expectedPendingRegisterChecks = listOf(firstRegisterCheckDto, secondRegisterCheckDto, thirdRegisterCheckDto)

            // When
            val actualPendingRegisterChecks = registerCheckService.getPendingRegisterChecks(certificateSerial, DEFAULT_PAGE_SIZE)

            // Then
            assertThat(actualPendingRegisterChecks).hasSize(expectedRecordCount)
            assertThat(actualPendingRegisterChecks)
                .isEqualTo(expectedPendingRegisterChecks)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verify(registerCheckRepository).findPendingEntriesByGssCodes(listOf(firstGssCodeFromEroApi, secondGssCodeFromEroApi, anotherGssCodeFromEroApi), DEFAULT_PAGE_SIZE)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(firstRegisterCheckEntity)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(secondRegisterCheckEntity)
            verify(pendingRegisterCheckMapper).registerCheckEntityToPendingRegisterCheckDto(thirdRegisterCheckEntity)
            verifyNoMoreInteractions(registerCheckRepository, pendingRegisterCheckMapper)
        }

        @Test
        fun `should throw IER not found exception given IER API client throws IER not found exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerEroNotFoundException(certificateSerial)
            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(IerEroNotFoundException::class.java) {
                registerCheckService.getPendingRegisterChecks(certificateSerial, DEFAULT_PAGE_SIZE)
            }

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verifyNoInteractions(registerCheckRepository, pendingRegisterCheckMapper)
        }

        @Test
        fun `should throw general IER exception given IER API client throws general exception`() {
            // Given
            val certificateSerial = "123456789"

            val expected = IerGeneralException("Error getting eroId for certificate serial $certificateSerial")
            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(IerGeneralException::class.java) {
                registerCheckService.getPendingRegisterChecks(certificateSerial, DEFAULT_PAGE_SIZE)
            }

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verifyNoInteractions(registerCheckRepository, pendingRegisterCheckMapper)
        }
    }

    @Nested
    inner class AdminGetPendingRegisterChecks {

        @Test
        fun `should get empty pending register check records for a given ERO ID given IER returns valid values`() {
            // Given
            val eroId = "south-testington"
            val gssCodeFromEroApi = getRandomGssCode()

            given(retrieveGssCodeService.getGssCodesFromEroId(eq(eroId))).willReturn(listOf(gssCodeFromEroApi))
            given(registerCheckRepository.adminFindPendingEntriesByGssCodes(eq(listOf(gssCodeFromEroApi)), any())).willReturn(emptyList())

            // When
            val actualPendingRegisterChecks = registerCheckService.adminGetPendingRegisterChecks(eroId)

            // Then
            assertThat(actualPendingRegisterChecks).isNotNull
            assertThat(actualPendingRegisterChecks).isEmpty()
            verify(retrieveGssCodeService).getGssCodesFromEroId(eroId)
            verify(registerCheckRepository).adminFindPendingEntriesByGssCodes(listOf(gssCodeFromEroApi))
            verifyNoInteractions(adminPendingRegisterCheckMapper)
        }

        @Test
        fun `should get one pending register check record for a given ERO ID given IER returns valid values`() {
            // Given
            val eroId = "south-testington"
            val gssCodeFromEroApi = getRandomGssCode()
            val expectedRecordCount = 1

            val matchedRegisterCheckEntity = buildRegisterCheck(
                gssCode = gssCodeFromEroApi,
                sourceType = uk.gov.dluhc.registercheckerapi.database.entity.SourceType.VOTER_CARD,
                status = PENDING
            )
            val expectedRegisterCheckDto = buildAdminPendingRegisterCheckDto(
                sourceReference = matchedRegisterCheckEntity.sourceReference,
                sourceType = uk.gov.dluhc.registercheckerapi.dto.SourceType.VOTER_CARD,
                gssCode = gssCodeFromEroApi
            )

            given(retrieveGssCodeService.getGssCodesFromEroId(eq(eroId))).willReturn(listOf(gssCodeFromEroApi))
            given(registerCheckRepository.adminFindPendingEntriesByGssCodes(eq(listOf(gssCodeFromEroApi)), any())).willReturn(listOf(matchedRegisterCheckEntity))
            given(adminPendingRegisterCheckMapper.registerCheckEntityToAdminPendingRegisterCheckDto(matchedRegisterCheckEntity)).willReturn(expectedRegisterCheckDto)

            val expectedPendingRegisterChecks = listOf(expectedRegisterCheckDto)

            // When
            val actualPendingRegisterChecks = registerCheckService.adminGetPendingRegisterChecks(eroId)

            // Then
            assertThat(actualPendingRegisterChecks).hasSize(expectedRecordCount)
            assertThat(actualPendingRegisterChecks)
                .isEqualTo(expectedPendingRegisterChecks)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
            verify(retrieveGssCodeService).getGssCodesFromEroId(eroId)
            verify(registerCheckRepository).adminFindPendingEntriesByGssCodes(listOf(gssCodeFromEroApi))
            verify(adminPendingRegisterCheckMapper).registerCheckEntityToAdminPendingRegisterCheckDto(matchedRegisterCheckEntity)
            verifyNoMoreInteractions(registerCheckRepository, adminPendingRegisterCheckMapper)
        }

        @Test
        fun `should get multiple pending register check records for a given ERO ID`() {
            // Given
            val eroId = "south-testington"
            val firstGssCodeFromEroApi = "E1234561"
            val secondGssCodeFromEroApi = "E567892"
            val anotherGssCodeFromEroApi = "E9876543" // No records will be matched for this gssCode
            val expectedRecordCount = 3

            val firstRegisterCheckEntity = buildRegisterCheck(
                gssCode = firstGssCodeFromEroApi,
                sourceType = uk.gov.dluhc.registercheckerapi.database.entity.SourceType.POSTAL_VOTE,
                status = PENDING
            )
            val secondRegisterCheckEntity = buildRegisterCheck(
                gssCode = firstGssCodeFromEroApi,
                sourceType = uk.gov.dluhc.registercheckerapi.database.entity.SourceType.PROXY_VOTE,
                status = PENDING
            )
            val thirdRegisterCheckEntity = buildRegisterCheck(
                gssCode = secondGssCodeFromEroApi,
                sourceType = uk.gov.dluhc.registercheckerapi.database.entity.SourceType.OVERSEAS_VOTE,
                status = PENDING
            )

            val firstRegisterCheckDto = buildAdminPendingRegisterCheckDto(
                gssCode = firstRegisterCheckEntity.gssCode,
                sourceType = uk.gov.dluhc.registercheckerapi.dto.SourceType.POSTAL_VOTE,
                sourceReference = firstRegisterCheckEntity.sourceReference
            )
            val secondRegisterCheckDto = buildAdminPendingRegisterCheckDto(
                gssCode = firstGssCodeFromEroApi,
                sourceType = uk.gov.dluhc.registercheckerapi.dto.SourceType.PROXY_VOTE,
                sourceReference = secondRegisterCheckEntity.sourceReference
            )
            val thirdRegisterCheckDto = buildAdminPendingRegisterCheckDto(
                gssCode = secondGssCodeFromEroApi,
                sourceType = uk.gov.dluhc.registercheckerapi.dto.SourceType.OVERSEAS_VOTE,
                sourceReference = thirdRegisterCheckEntity.sourceReference
            )

            val expectedGssCodes = listOf(firstGssCodeFromEroApi, secondGssCodeFromEroApi, anotherGssCodeFromEroApi)
            given(retrieveGssCodeService.getGssCodesFromEroId(eq(eroId))).willReturn(expectedGssCodes)
            given(registerCheckRepository.adminFindPendingEntriesByGssCodes(eq(expectedGssCodes), any())).willReturn(listOf(firstRegisterCheckEntity, secondRegisterCheckEntity, thirdRegisterCheckEntity))
            given(adminPendingRegisterCheckMapper.registerCheckEntityToAdminPendingRegisterCheckDto(eq(firstRegisterCheckEntity))).willReturn(firstRegisterCheckDto)
            given(adminPendingRegisterCheckMapper.registerCheckEntityToAdminPendingRegisterCheckDto(eq(secondRegisterCheckEntity))).willReturn(secondRegisterCheckDto)
            given(adminPendingRegisterCheckMapper.registerCheckEntityToAdminPendingRegisterCheckDto(eq(thirdRegisterCheckEntity))).willReturn(thirdRegisterCheckDto)

            val expectedPendingRegisterChecks = listOf(firstRegisterCheckDto, secondRegisterCheckDto, thirdRegisterCheckDto)

            // When
            val actualPendingRegisterChecks = registerCheckService.adminGetPendingRegisterChecks(eroId)

            // Then
            assertThat(actualPendingRegisterChecks).hasSize(expectedRecordCount)
            assertThat(actualPendingRegisterChecks)
                .isEqualTo(expectedPendingRegisterChecks)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
            verify(retrieveGssCodeService).getGssCodesFromEroId(eroId)
            verify(registerCheckRepository).adminFindPendingEntriesByGssCodes(listOf(firstGssCodeFromEroApi, secondGssCodeFromEroApi, anotherGssCodeFromEroApi))
            verify(adminPendingRegisterCheckMapper).registerCheckEntityToAdminPendingRegisterCheckDto(firstRegisterCheckEntity)
            verify(adminPendingRegisterCheckMapper).registerCheckEntityToAdminPendingRegisterCheckDto(secondRegisterCheckEntity)
            verify(adminPendingRegisterCheckMapper).registerCheckEntityToAdminPendingRegisterCheckDto(thirdRegisterCheckEntity)
            verifyNoMoreInteractions(registerCheckRepository, adminPendingRegisterCheckMapper)
        }

        @Test
        fun `should throw IER not found exception given IER API client throws IER not found exception`() {
            // Given
            val eroId = "south-testington"

            val expected = EroIdNotFoundException(eroId)
            given(retrieveGssCodeService.getGssCodesFromEroId(eq(eroId))).willThrow(expected)

            // When
            val ex = catchThrowableOfType(EroIdNotFoundException::class.java) {
                registerCheckService.adminGetPendingRegisterChecks(eroId)
            }

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(retrieveGssCodeService).getGssCodesFromEroId(eroId)
            verifyNoInteractions(registerCheckRepository, adminPendingRegisterCheckMapper)
        }

        @Test
        fun `should throw general IER exception given IER API client throws general exception`() {
            // Given
            val eroId = "south-testington"

            val expected = IerGeneralException("Error retrieving EROs from IER API")
            given(retrieveGssCodeService.getGssCodesFromEroId(eq(eroId))).willThrow(expected)

            // When
            val ex = catchThrowableOfType(IerGeneralException::class.java) {
                registerCheckService.adminGetPendingRegisterChecks(eroId)
            }

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(retrieveGssCodeService).getGssCodesFromEroId(eroId)
            verifyNoInteractions(registerCheckRepository, adminPendingRegisterCheckMapper)
        }
    }

    @Nested
    inner class SendConfirmRegisterCheckResultMessage {

        @ParameterizedTest
        @CsvSource(
            value = [
                "VOTER_CARD, VOTER_MINUS_CARD",
                "POSTAL_VOTE, POSTAL_MINUS_VOTE",
                "PROXY_VOTE, PROXY_MINUS_VOTE",
                "OVERSEAS_VOTE, OVERSEAS_MINUS_VOTE",
                "APPLICATIONS_API, APPLICATIONS_MINUS_API"
            ]
        )
        fun `should submit a ConfirmRegisterCheckResult Message for all services`(
            sourceTypeDto: SourceTypeEntity,
            sourceType: SourceType
        ) {
            // Given
            val requestId = randomUUID()
            val historicalSearchEarliestDate = Instant.now()
            val registerCheckMatchList = mutableListOf<RegisterCheckMatch>().apply { repeat(1) { add(buildRegisterCheckMatch()) } }
            val registerCheckMatchListDto = mutableListOf<RegisterCheckMatchDto>().apply { repeat(1) { add(buildRegisterCheckMatchDto()) } }
            val savedPendingRegisterCheckEntity = buildRegisterCheck(
                correlationId = requestId,
                matchCount = 1,
                status = EXACT_MATCH,
                registerCheckMatches = registerCheckMatchList,
                historicalSearchEarliestDate = historicalSearchEarliestDate,
                sourceType = sourceTypeDto
            )
            val expectedMessage = buildRegisterCheckResultMessage(
                sourceType = SourceType.VOTER_MINUS_CARD,
                sourceReference = savedPendingRegisterCheckEntity.sourceReference,
                sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
                registerCheckResult = RegisterCheckResult.EXACT_MINUS_MATCH,
                matches = registerCheckMatchListDto.map { buildVcaRegisterCheckMatchFromMatchDto(it) },
                historicalSearchEarliestDate = historicalSearchEarliestDate.atOffset(ZoneOffset.UTC)
            )

            given(registerCheckResultMessageMapper.fromRegisterCheckEntityToRegisterCheckResultMessage(any())).willReturn(expectedMessage)
            given(messageQueueResolver.getTargetQueueForSourceType(any())).willReturn(confirmRegisterCheckResultMessageQueue)

            // When
            registerCheckService.sendConfirmRegisterCheckResultMessage(savedPendingRegisterCheckEntity)

            // Then
            verify(registerCheckResultMessageMapper).fromRegisterCheckEntityToRegisterCheckResultMessage(savedPendingRegisterCheckEntity)
            verify(confirmRegisterCheckResultMessageQueue).submit(expectedMessage)
        }
    }

    @Nested
    inner class UpdatePendingRegisterCheck {

        @Test
        fun `should throw PendingRegisterCheckNotFoundException for a non-existing pending register check`() {
            // Given
            val certificateSerial = "123456789"
            val requestId = randomUUID()
            val expectedGssCode = getRandomGssCode()
            val registerCheckResultDto = buildRegisterCheckResultDto(requestId = requestId, correlationId = requestId, gssCode = expectedGssCode)
            val expected = PendingRegisterCheckNotFoundException(requestId)

            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(listOf(expectedGssCode))
            given(registerCheckRepository.findByCorrelationId(any())).willReturn(null)

            // When
            val ex = catchThrowableOfType(PendingRegisterCheckNotFoundException::class.java) {
                registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Pending register check for requestid:[$requestId] not found")
            verify(registerCheckRepository).findByCorrelationId(requestId)
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verifyNoInteractions(registerCheckResultMapper, confirmRegisterCheckResultMessageQueue)
        }

        @ParameterizedTest
        @EnumSource(
            value = CheckStatus::class,
            names = ["NO_MATCH", "EXACT_MATCH", "MULTIPLE_MATCH", "TOO_MANY_MATCHES", "ARCHIVED"]
        )
        fun `should throw RegisterCheckUnexpectedStatusException when existing register check status is not PENDING`(
            existingCheckStatusInDb: CheckStatus,
        ) {
            // Given
            val certificateSerial = "123456789"
            val requestId = randomUUID()
            val expectedGssCode = getRandomGssCode()
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                gssCode = expectedGssCode,
                registerCheckStatus = RegisterCheckStatus.EXACT_MATCH,
            )
            val expected = RegisterCheckUnexpectedStatusException(requestId, existingCheckStatusInDb)
            given(registerCheckRepository.findByCorrelationId(any())).willReturn(buildRegisterCheck(correlationId = requestId, status = existingCheckStatusInDb))
            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(listOf(expectedGssCode))

            // When
            val ex = catchThrowableOfType(RegisterCheckUnexpectedStatusException::class.java) {
                registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Register check with requestid:[$requestId] has an unexpected status:[$existingCheckStatusInDb]")
            verify(registerCheckRepository).findByCorrelationId(requestId)
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
            verifyNoInteractions(registerCheckResultMapper, confirmRegisterCheckResultMessageQueue)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "0, NO_MATCH, NO_MINUS_MATCH",
                "1, EXACT_MATCH, EXACT_MINUS_MATCH",
                "1, PENDING_DETERMINATION, PENDING_MINUS_DETERMINATION",
                "1, EXPIRED, EXPIRED",
                "1, NOT_STARTED, NOT_MINUS_STARTED",
                "10, MULTIPLE_MATCH, MULTIPLE_MINUS_MATCH",
                "11, TOO_MANY_MATCHES, TOO_MINUS_MANY_MINUS_MATCHES"
            ]
        )
        fun `should update pending register check successfully`(
            matchCount: Int,
            registerCheckStatus: RegisterCheckStatus,
            registerCheckResult: RegisterCheckResult,
        ) {
            // Given
            val certificateSerial = "123456789"
            val requestId = randomUUID()
            val matchingGssCode = getRandomGssCode()
            val otherGssCode = getRandomGssCode()
            val historicalSearchEarliestDate = Instant.now()
            val registerCheckMatchDtoList = mutableListOf<RegisterCheckMatchDto>().apply { repeat(matchCount) { add(buildRegisterCheckMatchDto()) } }
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                gssCode = matchingGssCode,
                matchResultSentAt = Instant.now(),
                matchCount = matchCount,
                registerCheckStatus = registerCheckStatus,
                registerCheckMatches = registerCheckMatchDtoList,
                historicalSearchEarliestDate = historicalSearchEarliestDate,
            )
            val savedPendingRegisterCheckEntity = buildRegisterCheck(
                correlationId = requestId,
                status = PENDING
            )

            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(listOf(matchingGssCode, otherGssCode))
            given(registerCheckRepository.findByCorrelationId(any())).willReturn(savedPendingRegisterCheckEntity)
            registerCheckMatchDtoList.forEach {
                given(registerCheckResultMapper.fromDtoToRegisterCheckMatchEntity(it)).willReturn(buildRegisterCheckMatch())
            }
            given(matchStatusResolver.resolveStatus(any(), any())).willReturn(registerCheckStatus)

            // When
            registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)

            // Then
            verify(registerCheckRepository).findByCorrelationId(requestId)
            verify(matchStatusResolver).resolveStatus(registerCheckResultDto, savedPendingRegisterCheckEntity)
            registerCheckMatchDtoList.forEach { verify(registerCheckResultMapper).fromDtoToRegisterCheckMatchEntity(it) }
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
        }

        @Test
        fun `should throw GssCodeMismatchException when gssCode from IER mismatches gssCode in payload`() {
            // Given
            val certificateSerial = "123456789"
            val requestId = randomUUID()
            val requestGssCode = "E12345678"
            val differentGssCodeFromEroApi = getRandomGssCode()
            val registerCheckResultDto = buildRegisterCheckResultDto(requestId = requestId, correlationId = requestId, gssCode = requestGssCode)
            val expected = GssCodeMismatchException(certificateSerial, requestGssCode)

            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willReturn(listOf(differentGssCodeFromEroApi))

            // When
            val ex = catchThrowableOfType(GssCodeMismatchException::class.java) {
                registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request gssCode:[E12345678] does not match with gssCode for certificateSerial:[123456789]")
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
        }

        @Test
        fun `should throw IER not found exception given IER API client throws IER not found exception`() {
            // Given
            val certificateSerial = "123456789"
            val registerCheckResultDto = buildRegisterCheckResultDto()

            val expected = IerEroNotFoundException(certificateSerial)
            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(IerEroNotFoundException::class.java) {
                registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)
            }

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
        }

        @Test
        fun `should throw general IER exception given IER API client throws general exception`() {
            // Given
            val certificateSerial = "123456789"
            val registerCheckResultDto = buildRegisterCheckResultDto()
            val expected = IerGeneralException("Error getting eroId for certificate serial 123456789")

            given(retrieveGssCodeService.getGssCodesFromCertificateSerial(any())).willThrow(expected)

            // When
            val ex = catchThrowableOfType(IerGeneralException::class.java) {
                registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)
            }

            // Then
            assertThat(ex).isEqualTo(expected)
            verify(retrieveGssCodeService).getGssCodesFromCertificateSerial(certificateSerial)
        }

        @Test
        fun `should audit request body`() {
            // Given
            val correlationId = randomUUID()
            val requestBodyJson = requestBodyJson(correlationId)

            // When
            registerCheckService.auditRequestBody(correlationId, requestBodyJson)

            // Then
            verify(registerCheckResultDataRepository).save(registerCheckRequestDataCaptor.capture())
            val registerCheckResultData = registerCheckRequestDataCaptor.value
            assertThat(registerCheckResultData.correlationId).isEqualTo(correlationId)
            assertThat(registerCheckResultData.requestBody).isEqualTo(requestBodyJson)
        }

        private fun requestBodyJson(requestId: UUID): String =
            """
                {
                "requestid": "$requestId",
                "gssCode": "T12345679",
                "createdAt": "2022-10-05T10:28:37.3052627+01:00",
                "registerCheckMatches": [],
                "registerCheckMatchCount": 0
                }
            """.trimIndent()
    }
}
