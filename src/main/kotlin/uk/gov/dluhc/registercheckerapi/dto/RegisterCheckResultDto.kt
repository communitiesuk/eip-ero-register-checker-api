package uk.gov.dluhc.registercheckerapi.dto

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class RegisterCheckResultDto(
    val requestId: UUID,
    val correlationId: UUID,
    val gssCode: String,
    val matchResultSentAt: Instant,
    val matchCount: Int,
    var registerCheckStatus: RegisterCheckStatus? = null,
    val registerCheckMatches: List<RegisterCheckMatchDto>?,
    val historicalSearchEarliestDate: Instant? = null,
)

data class RegisterCheckMatchDto(
    val emsElectorId: String,
    val attestationCount: Int,
    val personalDetail: PersonalDetailDto,
    val registeredStartDate: LocalDate?,
    val registeredEndDate: LocalDate?,
    val applicationCreatedAt: Instant?,
    val franchiseCode: String
)

enum class RegisterCheckStatus {
    NO_MATCH,
    EXACT_MATCH,
    PARTIAL_MATCH,
    MULTIPLE_MATCH,
    TOO_MANY_MATCHES,
    PENDING_DETERMINATION,
    EXPIRED,
    NOT_STARTED,
}
