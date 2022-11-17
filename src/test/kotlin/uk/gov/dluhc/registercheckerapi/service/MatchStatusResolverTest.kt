package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus.EXACT_MATCH
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus.PARTIAL_MATCH
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildAddressDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildAddress
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.time.LocalDate
import java.util.stream.Stream

internal class MatchStatusResolverTest {

    private val matchStatusResolver = MatchStatusResolver()

    @ParameterizedTest
    @CsvSource(
        value = [
            "0,   NO_MATCH",
            "2,   MULTIPLE_MATCH",
            "3,   MULTIPLE_MATCH",
            "4,   MULTIPLE_MATCH",
            "5,   MULTIPLE_MATCH",
            "6,   MULTIPLE_MATCH",
            "7,   MULTIPLE_MATCH",
            "8,   MULTIPLE_MATCH",
            "9,   MULTIPLE_MATCH",
            "10,  MULTIPLE_MATCH",
            "11,  TOO_MANY_MATCHES",
            "100, TOO_MANY_MATCHES",
        ]
    )
    fun `should resolve status given match count not 1`(matchCount: Int, expectedStatus: RegisterCheckStatus) {
        // Given
        val registerCheckResultDto = buildRegisterCheckResultDto(matchCount = matchCount)
        val registerCheck = buildRegisterCheck()

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheck)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "'',          ,  , EXACT_MATCH", // no franchise code nor start nor end date
            "' ',         , 2, EXACT_MATCH", // end date is in the future
            "' ',       -2,  , EXACT_MATCH", // start date is in the past
            "'   ',     -2, 2, EXACT_MATCH", // start/end dates in past/future means status is EXACT_MATCH
            "G,         -2, 2, EXACT_MATCH", // franchise code is still not blank nor "PENDING"
            "PENDING,   -2, 2, PENDING_DETERMINATION", // a franchise code of pending with valid dates means status is PENDING_DETERMINATION
            "PENDING,     ,  , PENDING_DETERMINATION", // a franchise code of pending with null dates means status is PENDING_DETERMINATION
            "'',         2, 2, NOT_STARTED", // start date in the future means status is NOT_STARTED
            "' ',       -2,-2, EXPIRED", // end date in the past means status is EXPIRED
            "'',         2,-2, NOT_STARTED", // start/end dates in future/past means status is NOT_STARTED
        ]
    )
    fun `should map api to dto for a given registerCheckMatchCount when it is 1 and not a partial match`(
        franchiseCode: String,
        relativeRegisteredStartDate: Long?,
        relativeRegisteredEndDate: Long?,
        expectedStatus: RegisterCheckStatus
    ) {
        // Given
        val registeredStartDate = relativeRegisteredStartDate?.let { LocalDate.now().plusDays(it) }
        val registeredEndDate = relativeRegisteredEndDate?.let { LocalDate.now().plusDays(it) }
        val registerCheckResultDto = buildRegisterCheckResultDto(
            matchCount = 1,
            registerCheckMatches = listOf(
                buildRegisterCheckMatchDto(
                    franchiseCode = franchiseCode,
                    registeredStartDate = registeredStartDate,
                    registeredEndDate = registeredEndDate
                )
            )
        )
        val personalDetailDto = registerCheckResultDto.registerCheckMatches!!.first().personalDetail
        val registerCheck = buildRegisterCheck(
            personalDetail = buildPersonalDetail(
                firstName = personalDetailDto.firstName,
                surname = personalDetailDto.surname,
                dateOfBirth = personalDetailDto.dateOfBirth,
                address = buildAddress(
                    street = personalDetailDto.address.street,
                    postcode = personalDetailDto.address.postcode,
                )
            )
        )

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheck)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @MethodSource("personalDetails")
    fun `should map api to dto given 1 match and partially matching details`(
        matchFirstName: String,
        applicationFirstName: String,
        matchSurname: String,
        applicationSurname: String,
        matchDateOfBirth: LocalDate?,
        applicationDateOfBirth: LocalDate?,
        matchPostcode: String,
        applicationPostcode: String,
        expectedStatus: RegisterCheckStatus
    ) {
        // Given
        val registerCheckResultDto = buildRegisterCheckResultDto(
            matchCount = 1,
            registerCheckMatches = listOf(
                buildRegisterCheckMatchDto(
                    personalDetail = buildPersonalDetailDto(
                        firstName = matchFirstName,
                        surname = matchSurname,
                        dateOfBirth = matchDateOfBirth,
                        address = buildAddressDto(
                            postcode = matchPostcode
                        )
                    )
                )
            )
        )
        val registerCheck = buildRegisterCheck(
            personalDetail = buildPersonalDetail(
                firstName = applicationFirstName,
                surname = applicationSurname,
                dateOfBirth = applicationDateOfBirth,
                address = buildAddress(
                    postcode = applicationPostcode
                )
            )
        )

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheck)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    companion object {
        @JvmStatic
        private fun personalDetails(): Stream<Arguments> {
            val dateOfBirth = LocalDate.of(1999, 11, 12)
            val anotherDateOfBirth = LocalDate.of(1970, 3, 1)
            return Stream.of(
                Arguments.of("Mike", "Mike", "Jones", "Jones", dateOfBirth, dateOfBirth, "L1 1AB", "L1 1AB", EXACT_MATCH),
                Arguments.of("Mike", "Mike", "Jones", "Jones", null, null, "L1 1AB", "L1 1AB", EXACT_MATCH),
                Arguments.of("Mike", "Matt", "Jones", "Jones", dateOfBirth, dateOfBirth, "L1 1AB", "L1 1AB", PARTIAL_MATCH),
                Arguments.of("Mike", "Mike", "Jones", "Smith", dateOfBirth, dateOfBirth, "L1 1AB", "L1 1AB", PARTIAL_MATCH),
                Arguments.of("Mike", "Mike", "Jones", "Jones", dateOfBirth, anotherDateOfBirth, "L1 1AB", "L1 1AB", PARTIAL_MATCH),
                Arguments.of("Mike", "Mike", "Jones", "Jones", null, dateOfBirth, "L1 1AB", "L1 1AB", PARTIAL_MATCH),
                Arguments.of("Mike", "Mike", "Jones", "Jones", dateOfBirth, null, "L1 1AB", "L1 1AB", PARTIAL_MATCH),
                Arguments.of("Mike", "Mike", "Jones", "Jones", dateOfBirth, dateOfBirth, "L1 1AB", "L2 2AB", PARTIAL_MATCH)
            )
        }
    }
}
