package uk.gov.dluhc.registercheckerapi.validator

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckMatchCountMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckResultDto
import java.util.UUID.randomUUID

internal class RegisterCheckRequestValidatorTest {

    private val registerCheckRequestValidator = RegisterCheckRequestValidator()

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
            val ex = catchThrowableOfType(RequestIdMismatchException::class.java) {
                registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request requestId:[$requestId] does not match with requestid:[$correlationId] in body payload")
        }

        @Test
        fun `should throw RegisterCheckMatchCountMismatchException when matchCount is 0 and registerCheckMatch contains entries`() {
            // Given
            val requestId = randomUUID()
            val matchCount = 0
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = listOf(buildRegisterCheckMatchDto())
            )
            val expected = RegisterCheckMatchCountMismatchException("Request [registerCheckMatches] array must be null or empty for [registerCheckMatchCount:$matchCount] in body payload")

            // When
            val ex = catchThrowableOfType(RegisterCheckMatchCountMismatchException::class.java) {
                registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request [registerCheckMatches] array must be null or empty for [registerCheckMatchCount:0] in body payload")
        }

        @ParameterizedTest
        @NullAndEmptySource
        fun `should validate successfully when matchCount is 0 and registerCheckMatch is null or empty`(matches: List<RegisterCheckMatchDto>?) {
            // Given
            val requestId = randomUUID()
            val matchCount = 0
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = matches
            )

            // When
            registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)

            // Then
        }

        @Test
        fun `should validate successfully when matchCount is 1 and registerCheckMatch is valid in payload`() {
            // Given
            val requestId = randomUUID()
            val matchCount = 1
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = listOf(buildRegisterCheckMatchDto())
            )

            // When
            registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)

            // Then
        }

        @ParameterizedTest
        @CsvSource(value = ["2", "3", "4", "5", "6", "7", "8", "9", "10"])
        fun `should validate successfully when matchCount is between 2 and 10 and registerCheckMatch is valid in payload`(matchCount: Int) {
            // Given
            val requestId = randomUUID()
            val matches = mutableListOf<RegisterCheckMatchDto>().apply { repeat(matchCount) { add(buildRegisterCheckMatchDto()) } }
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = matches

            )

            // When
            registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)

            // Then
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
            val ex = catchThrowableOfType(RegisterCheckMatchCountMismatchException::class.java) {
                registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request [registerCheckMatches:1] array size must be same as [registerCheckMatchCount:2] in body payload")
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
            val ex = catchThrowableOfType(RegisterCheckMatchCountMismatchException::class.java) {
                registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)
            }

            // Then
            assertThat(ex.message).isEqualTo(expected.message)
            assertThat(ex.message).isEqualTo("Request [registerCheckMatches] array must be null or empty for [registerCheckMatchCount:11] in body payload")
        }

        @ParameterizedTest
        @NullAndEmptySource
        fun `should validate successfully when matchCount is greater than 10 and registerCheckMatch is null or empty`(matches: List<RegisterCheckMatchDto>?) {
            // Given
            val requestId = randomUUID()
            val matchCount = 11
            val registerCheckResultDto = buildRegisterCheckResultDto(
                requestId = requestId,
                correlationId = requestId,
                matchCount = matchCount,
                registerCheckMatches = matches
            )

            // When
            registerCheckRequestValidator.validateRequestBody("123456789", registerCheckResultDto)

            // Then
        }
    }
}
