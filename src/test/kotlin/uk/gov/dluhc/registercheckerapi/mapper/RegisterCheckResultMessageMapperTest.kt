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
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetailWithOptionalFieldsAsNull
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckMatchModel
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckPersonalDetailFromEntity
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultMessage

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckResultMessageMapperTest {

    @Mock
    private lateinit var checkStatusMapper: CheckStatusMapper

    @InjectMocks
    private val mapper = RegisterCheckResultMessageMapperImpl()

    @Nested
    inner class FromRegisterCheckEntityToRegisterCheckResultMessage {

        @ParameterizedTest
        @CsvSource(
            value = [
                "EXACT_MATCH, EXACT_MATCH",
                "PENDING_DETERMINATION, PENDING_DETERMINATION",
                "EXPIRED, EXPIRED",
                "NOT_STARTED, NOT_STARTED"
            ]
        )
        fun `should map entity to message when exact match found`(initialStatus: CheckStatus, expectedStatus: RegisterCheckResult) {
            // Given
            val registerCheck = buildRegisterCheck(status = initialStatus, registerCheckMatches = mutableListOf(buildRegisterCheckMatch()))
            given(checkStatusMapper.toRegisterCheckStatusResultEnum(any())).willReturn(expectedStatus)

            val expected = buildRegisterCheckResultMessage(
                sourceType = RegisterCheckSourceType.VOTER_CARD,
                sourceReference = registerCheck.sourceReference,
                sourceCorrelationId = registerCheck.sourceCorrelationId,
                registerCheckResult = expectedStatus,
                matches = registerCheck.registerCheckMatches.map { registerCheckMatch ->
                    with(registerCheckMatch) {
                        buildRegisterCheckMatchModel(
                            personalDetail = buildRegisterCheckPersonalDetailFromEntity(personalDetail),
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
            verify(checkStatusMapper).toRegisterCheckStatusResultEnum(initialStatus)
        }

        @Test
        fun `should map entity to message when no match`() {
            // Given
            val registerCheck = buildRegisterCheck(status = CheckStatus.NO_MATCH, registerCheckMatches = mutableListOf())
            given(checkStatusMapper.toRegisterCheckStatusResultEnum(any())).willReturn(RegisterCheckResult.NO_MATCH)

            val expected = buildRegisterCheckResultMessage(
                sourceType = RegisterCheckSourceType.VOTER_CARD,
                sourceReference = registerCheck.sourceReference,
                sourceCorrelationId = registerCheck.sourceCorrelationId,
                registerCheckResult = RegisterCheckResult.NO_MATCH,
                matches = emptyList()
            )

            // When
            val actual = mapper.fromRegisterCheckEntityToRegisterCheckResultMessage(registerCheck)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            assertThat(actual.matches).isNotNull
            assertThat(actual.matches).isEmpty()
            verify(checkStatusMapper).toRegisterCheckStatusResultEnum(CheckStatus.NO_MATCH)
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
            given(checkStatusMapper.toRegisterCheckStatusResultEnum(any())).willReturn(RegisterCheckResult.MULTIPLE_MATCH)

            val expected = buildRegisterCheckResultMessage(
                sourceType = RegisterCheckSourceType.VOTER_CARD,
                sourceReference = registerCheck.sourceReference,
                sourceCorrelationId = registerCheck.sourceCorrelationId,
                registerCheckResult = RegisterCheckResult.MULTIPLE_MATCH,
                matches = registerCheck.registerCheckMatches.map { registerCheckMatch ->
                    with(registerCheckMatch) {
                        buildRegisterCheckMatchModel(
                            personalDetail = buildRegisterCheckPersonalDetailFromEntity(personalDetail),
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
            verify(checkStatusMapper).toRegisterCheckStatusResultEnum(CheckStatus.MULTIPLE_MATCH)
        }
    }
}
