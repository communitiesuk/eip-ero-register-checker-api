package uk.gov.dluhc.registercheckerapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckRemovalDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.dto.SourceType as SourceTypeDtoEnum

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckRemovalServiceTest {

    @Mock
    private lateinit var registerCheckRepository: RegisterCheckRepository

    @Mock
    private lateinit var registerCheckResultDataRepository: RegisterCheckResultDataRepository

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private lateinit var registerCheckRemovalService: RegisterCheckRemovalService

    @Nested
    inner class RemoveRegisterCheckData {

        @Test
        fun `should not delete any records for a non-existing register check`() {
            // Given
            val dto = buildRegisterCheckRemovalDto()
            val entitySourceType = SourceType.VOTER_CARD

            given(sourceTypeMapper.fromDtoToEntityEnum(any())).willReturn(entitySourceType)
            given(registerCheckRepository.findBySourceReferenceAndSourceTypeAndGssCode(any(), any(), any())).willReturn(emptyList())

            // When
            registerCheckRemovalService.removeRegisterCheckData(dto)

            // Then
            verify(sourceTypeMapper).fromDtoToEntityEnum(SourceTypeDtoEnum.VOTER_CARD)
            verify(registerCheckRepository).findBySourceReferenceAndSourceTypeAndGssCode(dto.sourceReference, entitySourceType, dto.gssCode)
            verify(registerCheckRepository, never()).deleteAll(any())
            verifyNoInteractions(registerCheckResultDataRepository)
        }

        @Test
        fun `should delete records when both matching register check and register check result exists`() {
            // Given
            val dto = buildRegisterCheckRemovalDto()
            val entitySourceType = SourceType.VOTER_CARD
            val registerCheck1 = buildRegisterCheck()
            val registerCheck2 = buildRegisterCheck()
            val matchedRecordsList = listOf(registerCheck1, registerCheck2)

            val registerCheckResult1 = buildRegisterCheckResultData()
            val registerCheckResult2 = buildRegisterCheckResultData()
            val matchedCheckResultList = listOf(registerCheckResult1, registerCheckResult2)

            given(sourceTypeMapper.fromDtoToEntityEnum(any())).willReturn(entitySourceType)
            given(registerCheckRepository.findBySourceReferenceAndSourceTypeAndGssCode(any(), any(), any())).willReturn(matchedRecordsList)
            given(registerCheckResultDataRepository.findByCorrelationIdIn(any())).willReturn(matchedCheckResultList)

            // When
            registerCheckRemovalService.removeRegisterCheckData(dto)

            // Then
            verify(sourceTypeMapper).fromDtoToEntityEnum(SourceTypeDtoEnum.VOTER_CARD)
            verify(registerCheckRepository).findBySourceReferenceAndSourceTypeAndGssCode(dto.sourceReference, entitySourceType, dto.gssCode)
            verify(registerCheckRepository).deleteAll(matchedRecordsList)
            verify(registerCheckResultDataRepository).findByCorrelationIdIn(setOf(registerCheck1.correlationId, registerCheck2.correlationId))
            verify(registerCheckResultDataRepository).deleteAll(matchedCheckResultList)
        }

        @Test
        fun `should delete records when only matching register check exists with no register check result`() {
            // Given
            val dto = buildRegisterCheckRemovalDto()
            val entitySourceType = SourceType.VOTER_CARD
            val registerCheck1 = buildRegisterCheck()
            val registerCheck2 = buildRegisterCheck()
            val matchedRecordsList = listOf(registerCheck1, registerCheck2)

            given(sourceTypeMapper.fromDtoToEntityEnum(any())).willReturn(entitySourceType)
            given(registerCheckRepository.findBySourceReferenceAndSourceTypeAndGssCode(any(), any(), any())).willReturn(matchedRecordsList)
            given(registerCheckResultDataRepository.findByCorrelationIdIn(any())).willReturn(emptyList())

            // When
            registerCheckRemovalService.removeRegisterCheckData(dto)

            // Then
            verify(sourceTypeMapper).fromDtoToEntityEnum(SourceTypeDtoEnum.VOTER_CARD)
            verify(registerCheckRepository).findBySourceReferenceAndSourceTypeAndGssCode(dto.sourceReference, entitySourceType, dto.gssCode)
            verify(registerCheckRepository).deleteAll(matchedRecordsList)
            verify(registerCheckResultDataRepository).findByCorrelationIdIn(setOf(registerCheck1.correlationId, registerCheck2.correlationId))
            verify(registerCheckResultDataRepository, never()).deleteAll(any())
        }
    }
}
