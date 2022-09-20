package uk.gov.dluhc.registercheckerapi.dto

import java.time.Instant
import java.util.UUID

data class RegisterCheckDto(
    val correlationId: UUID,
    val sourceReference: String,
    val sourceCorrelationId: UUID,
    val sourceType: SourceType,
    val gssCode: String,
    val status: CheckStatus,
    val personalDetail: PersonalDetailDto,
    val createdBy: String,
    val createdAt: Instant
)

enum class SourceType {
    VOTER_CARD
}

enum class CheckStatus {
    PENDING
}
