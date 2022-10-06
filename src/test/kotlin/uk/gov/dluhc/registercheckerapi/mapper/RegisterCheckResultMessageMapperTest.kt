package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckAddress
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultMessage

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckResultMessageMapperTest {

    @Mock
    private lateinit var checkStatusMapper: CheckStatusMapper

    @InjectMocks
    private val mapper = RegisterCheckResultMessageMapperImpl()

    @Nested
    inner class FromRegisterCheckEntityToRegisterCheckResultMessage {

        @Test
        fun `should map entity to message when exact match found`() {
            // Given
            val registerCheck = buildRegisterCheck(status = CheckStatus.EXACT_MATCH, registerCheckMatches = mutableListOf(buildRegisterCheckMatch()))
            given(checkStatusMapper.toRegisterCheckStatusResultEnum(any())).willReturn(RegisterCheckResult.EXACT_MATCH)

            val expected = buildRegisterCheckResultMessage(
                sourceType = RegisterCheckSourceType.VOTER_CARD,
                sourceReference = registerCheck.sourceReference,
                sourceCorrelationId = registerCheck.sourceCorrelationId,
                registerCheckResult = RegisterCheckResult.EXACT_MATCH,
                matches = registerCheck.registerCheckMatches.map { registerCheckMatch ->
                    with(registerCheckMatch.personalDetail) {
                        buildRegisterCheckPersonalDetail(
                            firstName = firstName,
                            middleNames = middleNames,
                            surname = surname,
                            dateOfBirth = dateOfBirth,
                            phone = phoneNumber,
                            email = email,
                            address = with(address) {
                                buildRegisterCheckAddress(
                                    property = property, street = street, locality = locality, town = town,
                                    area = area, postcode = postcode, uprn = uprn
                                )
                            }
                        )
                    }
                }
            )

            // When
            val actual = mapper.fromRegisterCheckEntityToRegisterCheckResultMessage(registerCheck)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(checkStatusMapper).toRegisterCheckStatusResultEnum(CheckStatus.EXACT_MATCH)
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
                    with(registerCheckMatch.personalDetail) {
                        buildRegisterCheckPersonalDetail(
                            firstName = firstName,
                            middleNames = null,
                            surname = surname,
                            dateOfBirth = null,
                            phone = null,
                            email = null,
                            address = with(address) {
                                buildRegisterCheckAddress(
                                    property = null,
                                    street = street,
                                    locality = null,
                                    town = null,
                                    area = null,
                                    postcode = postcode,
                                    uprn = null
                                )
                            }
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
