package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.dto.VotingArrangementDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun buildRegisterCheckResultDto(
    requestId: UUID = UUID.randomUUID(),
    correlationId: UUID = requestId,
    gssCode: String = "E09000021",
    matchResultSentAt: Instant = Instant.now(),
    matchCount: Int = 1,
    registerCheckStatus: RegisterCheckStatus = RegisterCheckStatus.EXACT_MATCH,
    registerCheckMatches: List<RegisterCheckMatchDto>? = listOf(buildRegisterCheckMatchDto()),
    historicalSearchEarliestDate: Instant? = Instant.now()
) = RegisterCheckResultDto(
    requestId = requestId,
    correlationId = correlationId,
    gssCode = gssCode,
    matchResultSentAt = matchResultSentAt,
    matchCount = matchCount,
    registerCheckStatus = registerCheckStatus,
    registerCheckMatches = registerCheckMatches,
    historicalSearchEarliestDate = historicalSearchEarliestDate,
)

fun buildRegisterCheckMatchDto(
    emsElectorId: String = "EMS123456789",
    attestationCount: Int = 0,
    personalDetail: PersonalDetailDto = buildPersonalDetailDto(),
    registeredStartDate: LocalDate? = LocalDate.now().minusDays(1),
    registeredEndDate: LocalDate? = LocalDate.now().plusDays(1),
    applicationCreatedAt: Instant? = Instant.now(),
    franchiseCode: String = "Franchise123",
    postalVote: VotingArrangementDto? = buildVotingArrangementDto(),
    proxyVote: VotingArrangementDto? = null
) = RegisterCheckMatchDto(
    emsElectorId = emsElectorId,
    attestationCount = attestationCount,
    personalDetail = personalDetail,
    registeredStartDate = registeredStartDate,
    registeredEndDate = registeredEndDate,
    applicationCreatedAt = applicationCreatedAt,
    franchiseCode = franchiseCode,
    postalVote = postalVote,
    proxyVote = proxyVote,
)
