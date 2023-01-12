package uk.gov.dluhc.registercheckerapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckRemovalDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.SourceType as SourceTypeDtoEnum

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckRemovalServiceTest {

    @Mock
    private lateinit var registerCheckRepository: RegisterCheckRepository

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private lateinit var registerCheckRemovalService: RegisterCheckRemovalService

    @Nested
    inner class RemoveRegisterCheckData {

        @ParameterizedTest
        @NullAndEmptySource
        fun `should not delete any records for a non-existing register check`(
            matchedRecordsList: List<RegisterCheck>?
        ) {
            // Given
            val dto = buildRegisterCheckRemovalDto()
            val entitySourceType = SourceType.VOTER_CARD

            given(registerCheckRepository.findBySourceTypeAndSourceReference(any(), any())).willReturn(
                matchedRecordsList
            )
            given(sourceTypeMapper.fromDtoToEntityEnum(any())).willReturn(entitySourceType)

            // When
            registerCheckRemovalService.removeRegisterCheckData(dto)

            // Then
            verify(registerCheckRepository).findBySourceTypeAndSourceReference(entitySourceType, dto.sourceReference)
            verify(registerCheckRepository, never()).deleteAll(any())
            verify(sourceTypeMapper).fromDtoToEntityEnum(SourceTypeDtoEnum.VOTER_CARD)
        }

        @Test
        fun `should delete records for a matching register check`() {
            // Given
            val dto = buildRegisterCheckRemovalDto()
            val entitySourceType = SourceType.VOTER_CARD
            val matchedRecordsList = listOf(buildRegisterCheck(), buildRegisterCheck())

            given(registerCheckRepository.findBySourceTypeAndSourceReference(any(), any())).willReturn(
                matchedRecordsList
            )
            given(sourceTypeMapper.fromDtoToEntityEnum(any())).willReturn(entitySourceType)

            // When
            registerCheckRemovalService.removeRegisterCheckData(dto)

            // Then
            verify(registerCheckRepository).findBySourceTypeAndSourceReference(entitySourceType, dto.sourceReference)
            verify(registerCheckRepository).deleteAll(matchedRecordsList)
            verify(sourceTypeMapper).fromDtoToEntityEnum(SourceTypeDtoEnum.VOTER_CARD)
        }
    }
}
