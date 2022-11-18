package uk.gov.dluhc.registercheckerapi.service

import org.apache.commons.lang3.StringUtils.equalsIgnoreCase
import org.springframework.stereotype.Component
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import java.time.LocalDate

/**
 * Determines the status for a match result by checking if it is pending/not started or expired. Also checks if
 * specific personal details (e.g. surname, postcode) that are provided by the EMS match those on our system for the
 * application concerned.
 */
@Component
class MatchStatusResolver {

    fun resolveStatus(registerCheckResultDto: RegisterCheckResultDto, registerCheck: RegisterCheck): RegisterCheckStatus {
        return when (registerCheckResultDto.matchCount) {
            0 -> RegisterCheckStatus.NO_MATCH
            1 -> evaluateRegisterCheckStatusWithOneMatch(
                registerCheckResultDto.registerCheckMatches!!.first(),
                registerCheck.personalDetail
            )

            in 2..10 -> RegisterCheckStatus.MULTIPLE_MATCH
            else -> RegisterCheckStatus.TOO_MANY_MATCHES
        }
    }

    private fun evaluateRegisterCheckStatusWithOneMatch(
        registerCheckMatchDto: RegisterCheckMatchDto,
        personalDetailEntity: PersonalDetail
    ): RegisterCheckStatus =
        with(registerCheckMatchDto) {
            return if (equalsIgnoreCase(franchiseCode.trim(), "PENDING")) {
                RegisterCheckStatus.PENDING_DETERMINATION
            } else {
                val now = LocalDate.now()
                if (registeredStartDate?.isAfter(now) == true) {
                    RegisterCheckStatus.NOT_STARTED
                } else if (registeredEndDate?.isBefore(now) == true) {
                    RegisterCheckStatus.EXPIRED
                } else if (isPartialMatch(registerCheckMatchDto.personalDetail, personalDetailEntity)) {
                    RegisterCheckStatus.PARTIAL_MATCH
                } else {
                    RegisterCheckStatus.EXACT_MATCH
                }
            }
        }

    private fun isPartialMatch(personalDetailDto: PersonalDetailDto, personalDetailEntity: PersonalDetail): Boolean =
        !equalsIgnoreCase(personalDetailDto.firstName.trim(), personalDetailEntity.firstName.trim()) ||
            !equalsIgnoreCase(personalDetailDto.surname.trim(), personalDetailEntity.surname.trim()) ||
            sanitizePostcode(personalDetailDto.address.postcode) != sanitizePostcode(personalDetailEntity.address.postcode) ||
            personalDetailDto.dateOfBirth != personalDetailEntity.dateOfBirth

    private fun sanitizePostcode(postcode: String): String {
        return postcode.trim().uppercase().replace(Regex("[ ]+"), "")
    }
}
