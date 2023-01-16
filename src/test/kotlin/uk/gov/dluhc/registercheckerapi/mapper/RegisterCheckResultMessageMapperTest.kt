package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetailWithOptionalFieldsAsNull
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildVcaRegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildVcaRegisterCheckPersonalDetailSqsFromEntity
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.SourceType as SourceTypeVcaEnum

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckResultMessageMapperTest {

    @Mock
    private lateinit var checkStatusMapper: CheckStatusMapper

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private val mapper = RegisterCheckResultMessageMapperImpl()

    @Nested
    inner class FromRegisterCheckEntityToRegisterCheckResultMessage {

        @ParameterizedTest
        @CsvSource(
            value = [
                "EXACT_MATCH, EXACT_MINUS_MATCH",
                "NO_MATCH, NO_MINUS_MATCH",
                "PARTIAL_MATCH, PARTIAL_MINUS_MATCH",
                "PENDING_DETERMINATION, PENDING_MINUS_DETERMINATION",
                "EXPIRED, EXPIRED",
                "NOT_STARTED, NOT_MINUS_STARTED"
            ]
        )
        fun `should map entity to message when one match found`(
            initialStatus: CheckStatus,
            expectedStatus: RegisterCheckResult
        ) {
            // Given
            val registerCheckEntity = buildRegisterCheck(status = initialStatus, registerCheckMatches = mutableListOf(buildRegisterCheckMatch()))
            given(checkStatusMapper.toRegisterCheckResultEnum(any())).willReturn(expectedStatus)
            given(sourceTypeMapper.fromEntityToVcaSqsEnum(any())).willReturn(SourceTypeVcaEnum.VOTER_MINUS_CARD)

            val expectedMessage = buildRegisterCheckResultMessage(
                sourceType = SourceTypeVcaEnum.VOTER_MINUS_CARD,
                sourceReference = registerCheckEntity.sourceReference,
                sourceCorrelationId = registerCheckEntity.sourceCorrelationId,
                registerCheckResult = expectedStatus,
                matches = registerCheckEntity.registerCheckMatches.map { registerCheckMatch ->
                    with(registerCheckMatch) {
                        buildVcaRegisterCheckMatch(
                            personalDetail = buildVcaRegisterCheckPersonalDetailSqsFromEntity(personalDetail),
                            emsElectoralId = emsElectorId,
                            franchiseCode = franchiseCode ?: "",
                            registeredStartDate = registeredStartDate,
                            registeredEndDate = registeredEndDate,
                        )
                    }
                }
            )

            // When
            val actual = mapper.fromRegisterCheckEntityToRegisterCheckResultMessage(registerCheckEntity)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expectedMessage)
            verify(checkStatusMapper).toRegisterCheckResultEnum(initialStatus)
            verify(sourceTypeMapper).fromEntityToVcaSqsEnum(SourceType.VOTER_CARD)
            verifyNoMoreInteractions(sourceTypeMapper)
        }

        @Test
        fun `should map entity to message when no match`() {
            // Given
            val registerCheck = buildRegisterCheck(status = CheckStatus.NO_MATCH, registerCheckMatches = mutableListOf())
            given(checkStatusMapper.toRegisterCheckResultEnum(any())).willReturn(RegisterCheckResult.NO_MINUS_MATCH)
            given(sourceTypeMapper.fromEntityToVcaSqsEnum(any())).willReturn(SourceTypeVcaEnum.VOTER_MINUS_CARD)

            val expected = buildRegisterCheckResultMessage(
                sourceType = SourceTypeVcaEnum.VOTER_MINUS_CARD,
                sourceReference = registerCheck.sourceReference,
                sourceCorrelationId = registerCheck.sourceCorrelationId,
                registerCheckResult = RegisterCheckResult.NO_MINUS_MATCH,
                matches = emptyList()
            )

            // When
            val actual = mapper.fromRegisterCheckEntityToRegisterCheckResultMessage(registerCheck)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            assertThat(actual.matches).isNotNull
            assertThat(actual.matches).isEmpty()
            verify(checkStatusMapper).toRegisterCheckResultEnum(CheckStatus.NO_MATCH)
            verify(sourceTypeMapper).fromEntityToVcaSqsEnum(SourceType.VOTER_CARD)
            verifyNoMoreInteractions(sourceTypeMapper)
        }

        @Test
        fun `should map entity to message when optional fields are null`() {
            // Given
            val registerCheck = buildRegisterCheck(
                status = CheckStatus.MULTIPLE_MATCH,
                registerCheckMatches = mutableListOf(
                    buildRegisterCheckMatch(personalDetail = buildPersonalDetailWithOptionalFieldsAsNull()),
                    buildRegisterCheckMatch(personalDetail = buildPersonalDetailWithOptionalFieldsAsNull())
                )
            )
            given(checkStatusMapper.toRegisterCheckResultEnum(any())).willReturn(RegisterCheckResult.MULTIPLE_MINUS_MATCH)
            given(sourceTypeMapper.fromEntityToVcaSqsEnum(any())).willReturn(SourceTypeVcaEnum.VOTER_MINUS_CARD)

            val expected = buildRegisterCheckResultMessage(
                sourceType = SourceTypeVcaEnum.VOTER_MINUS_CARD,
                sourceReference = registerCheck.sourceReference,
                sourceCorrelationId = registerCheck.sourceCorrelationId,
                registerCheckResult = RegisterCheckResult.MULTIPLE_MINUS_MATCH,
                matches = registerCheck.registerCheckMatches.map { registerCheckMatch ->
                    with(registerCheckMatch) {
                        buildVcaRegisterCheckMatch(
                            personalDetail = buildVcaRegisterCheckPersonalDetailSqsFromEntity(personalDetail),
                            emsElectoralId = emsElectorId,
                            franchiseCode = franchiseCode ?: "",
                            registeredStartDate = registeredStartDate,
                            registeredEndDate = registeredEndDate,
                        )
                    }
                }
            )

            // When
            val actual = mapper.fromRegisterCheckEntityToRegisterCheckResultMessage(registerCheck)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            assertThat(actual.matches).hasSize(2)
            verify(checkStatusMapper).toRegisterCheckResultEnum(CheckStatus.MULTIPLE_MATCH)
            verify(sourceTypeMapper).fromEntityToVcaSqsEnum(SourceType.VOTER_CARD)
            verifyNoMoreInteractions(sourceTypeMapper)
        }
    }
}
