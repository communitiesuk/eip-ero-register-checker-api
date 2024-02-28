package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus.*
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
        val registerCheckEntity = buildRegisterCheck()

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheckEntity)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @MethodSource("matchDetails")
    fun `should map api to dto given 1 match and exactly matching personal details`(
        franchiseCode: String,
        relativeRegisteredStartDate: Long?,
        relativeRegisteredEndDate: Long?,
        isHistoricalSearch: Boolean?,
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
        val registerCheckEntity = buildRegisterCheck(
            personalDetail = buildPersonalDetail(
                firstName = personalDetailDto.firstName,
                surname = personalDetailDto.surname,
                dateOfBirth = personalDetailDto.dateOfBirth,
                address = buildAddress(
                    uprn = personalDetailDto.address.uprn,
                    property = personalDetailDto.address.property,
                    street = personalDetailDto.address.street,
                    postcode = personalDetailDto.address.postcode,
                )
            ),
            historicalSearch = isHistoricalSearch == true,
        )

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheckEntity)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @MethodSource("personalDetails")
    fun `should map api to dto given 1 match and partially matching personal details`(
        matchFirstName: String,
        applicationFirstName: String,
        matchSurname: String,
        applicationSurname: String,
        matchDateOfBirth: LocalDate?,
        applicationDateOfBirth: LocalDate?,
        expectedStatus: RegisterCheckStatus
    ) {
        // Given
        val addressDto = buildAddressDto()
        val registerCheckResultDto = buildRegisterCheckResultDto(
            matchCount = 1,
            registerCheckMatches = listOf(
                buildRegisterCheckMatchDto(
                    personalDetail = buildPersonalDetailDto(
                        firstName = matchFirstName,
                        surname = matchSurname,
                        dateOfBirth = matchDateOfBirth,
                        address = addressDto
                    )
                )
            )
        )
        val registerCheckEntity = buildRegisterCheck(
            personalDetail = buildPersonalDetail(
                firstName = applicationFirstName,
                surname = applicationSurname,
                dateOfBirth = applicationDateOfBirth,
                // use matching address details (not the focus of this method)
                address = buildAddress(
                    uprn = addressDto.uprn,
                    property = addressDto.property,
                    street = addressDto.street,
                    postcode = addressDto.postcode
                )
            )
        )

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheckEntity)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @MethodSource("addressDetails")
    fun `should map api to dto given 1 match and partially matching address details`(
        matchUprn: String?,
        applicationUprn: String?,
        matchProperty: String?,
        applicationProperty: String?,
        matchStreet: String,
        applicationStreet: String,
        matchPostcode: String,
        applicationPostcode: String,
        expectedStatus: RegisterCheckStatus
    ) {
        // Given
        val personalDetailDto = buildPersonalDetailDto(
            address = buildAddressDto(
                uprn = matchUprn,
                property = matchProperty,
                street = matchStreet,
                postcode = matchPostcode
            )
        )
        val registerCheckResultDto = buildRegisterCheckResultDto(
            matchCount = 1,
            registerCheckMatches = listOf(
                buildRegisterCheckMatchDto(personalDetail = personalDetailDto)
            )
        )
        val registerCheckEntity = buildRegisterCheck(
            // use matching personal details (focussing on address in this test)
            personalDetail = buildPersonalDetail(
                firstName = personalDetailDto.firstName,
                surname = personalDetailDto.surname,
                dateOfBirth = personalDetailDto.dateOfBirth,
                address = buildAddress(
                    uprn = applicationUprn,
                    property = applicationProperty,
                    street = applicationStreet,
                    postcode = applicationPostcode
                )
            )
        )

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheckEntity)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @MethodSource("sanitizedFields")
    fun `should map api to dto given 1 match and partially matching sanitized fields`(
        matchSurname: String,
        applicationSurname: String,
        matchPostcode: String,
        applicationPostcode: String,
        expectedStatus: RegisterCheckStatus
    ) {
        // Given
        val addressDto = buildAddressDto(
            postcode = matchPostcode
        )
        val personalDetailDto = buildPersonalDetailDto(
            surname = matchSurname,
            address = addressDto
        )
        val registerCheckResultDto = buildRegisterCheckResultDto(
            matchCount = 1,
            registerCheckMatches = listOf(
                buildRegisterCheckMatchDto(personalDetail = personalDetailDto)
            )
        )
        val registerCheckEntity = buildRegisterCheck(
            // use matching personal details (focussing on address in this test)
            personalDetail = buildPersonalDetail(
                firstName = personalDetailDto.firstName,
                surname = applicationSurname,
                dateOfBirth = personalDetailDto.dateOfBirth,
                address = buildAddress(
                    uprn = addressDto.uprn,
                    property = addressDto.property,
                    street = addressDto.street,
                    postcode = applicationPostcode
                )
            )
        )

        // When
        val status = matchStatusResolver.resolveStatus(registerCheckResultDto, registerCheckEntity)

        // Then
        assertThat(status).isEqualTo(expectedStatus)
    }

    companion object {
        @JvmStatic
        private fun matchDetails(): Stream<Arguments> {
            val twoDaysAgo = -2L
            val twoDaysFromNow = 2L
            val emptyFranchiseCode = ""
            val pendingFranchiseCode = "PENDING"
            val isHistoricalSearch = true
            return Stream.of(
                Arguments.of(emptyFranchiseCode, null, null, null, EXACT_MATCH),
                Arguments.of(emptyFranchiseCode, null, twoDaysFromNow, null, EXACT_MATCH),
                Arguments.of(emptyFranchiseCode, twoDaysAgo, null, null, EXACT_MATCH),
                Arguments.of(emptyFranchiseCode, twoDaysAgo, twoDaysFromNow, null, EXACT_MATCH),
                Arguments.of("G", twoDaysAgo, twoDaysFromNow, null, EXACT_MATCH),
                Arguments.of(pendingFranchiseCode, twoDaysAgo, twoDaysFromNow, null, PENDING_DETERMINATION),
                Arguments.of(pendingFranchiseCode, null, null, null, PENDING_DETERMINATION),
                Arguments.of(emptyFranchiseCode, twoDaysFromNow, twoDaysFromNow, null, NOT_STARTED),
                Arguments.of(emptyFranchiseCode, twoDaysAgo, twoDaysAgo, null, EXPIRED),
                Arguments.of(emptyFranchiseCode, twoDaysAgo, twoDaysAgo, isHistoricalSearch, EXACT_MATCH),
                Arguments.of(emptyFranchiseCode, twoDaysFromNow, twoDaysFromNow, null, NOT_STARTED),
            )
        }

        @JvmStatic
        private fun personalDetails(): Stream<Arguments> {
            val firstName = "David"
            val surname = "Jones"
            val dateOfBirth = LocalDate.of(1999, 11, 12)
            val anotherDateOfBirth = LocalDate.of(1970, 3, 1)
            return Stream.of(
                Arguments.of(firstName, firstName.uppercase(), surname, surname, dateOfBirth, dateOfBirth, EXACT_MATCH),
                Arguments.of(firstName, firstName, surname, surname.uppercase(), null, null, EXACT_MATCH),
                Arguments.of(firstName, "Fred", surname, surname, dateOfBirth, dateOfBirth, PARTIAL_MATCH),
                Arguments.of(firstName, firstName, surname, "Smith", dateOfBirth, dateOfBirth, PARTIAL_MATCH),
                Arguments.of(firstName, firstName, surname, surname, dateOfBirth, anotherDateOfBirth, PARTIAL_MATCH),
                Arguments.of(firstName, firstName, surname, surname, null, dateOfBirth, PARTIAL_MATCH)
            )
        }

        @JvmStatic
        private fun addressDetails(): Stream<Arguments> {
            val uprn = "200003393492"
            val property = "The House"
            val street = "1 The Street"
            val postcode = "L1 1AB"
            return Stream.of(
                Arguments.of(uprn, uprn, property, property, street, street, postcode, postcode, EXACT_MATCH),
                Arguments.of(uprn, uprn, "", property, "", street, "", postcode, EXACT_MATCH),
                Arguments.of("000$uprn", uprn, "", property, "", street, "", postcode, EXACT_MATCH),
                Arguments.of(null, null, property, property.uppercase(), street, street, postcode, postcode, EXACT_MATCH),
                Arguments.of(null, null, property, property, street, street.uppercase(), postcode, postcode, EXACT_MATCH),
                Arguments.of(uprn, null, null, null, street, street, postcode, postcode.lowercase(), EXACT_MATCH),
                Arguments.of(null, null, property, "The Flat", street, street, postcode, postcode, PARTIAL_MATCH),
                Arguments.of(null, null, property, null, street, street, postcode, postcode, PARTIAL_MATCH),
                Arguments.of(null, null, property, property, street, "2 The Lane", postcode, postcode, PARTIAL_MATCH),
                Arguments.of(null, null, property, property, street, street, postcode, "L2 2AB", PARTIAL_MATCH)
            )
        }

        @JvmStatic
        private fun sanitizedFields(): Stream<Arguments> {
            val surname = "O'Brien"
            val postcode = "L1 1AB"
            return Stream.of(
                Arguments.of(surname, surname.uppercase(), postcode, postcode, EXACT_MATCH),
                Arguments.of(surname, "OBRIEN", postcode, postcode, EXACT_MATCH),
                Arguments.of("Jones-Smith", "JONES SMITH", postcode, postcode, EXACT_MATCH),
                Arguments.of(surname, surname, postcode, postcode.lowercase(), EXACT_MATCH),
                Arguments.of(surname, surname, postcode, "l11ab", EXACT_MATCH)
            )
        }
    }
}
