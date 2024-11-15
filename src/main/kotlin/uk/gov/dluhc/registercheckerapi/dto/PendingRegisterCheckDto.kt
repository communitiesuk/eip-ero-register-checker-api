package uk.gov.dluhc.registercheckerapi.dto

import java.time.Instant
import java.util.UUID

data class PendingRegisterCheckDto(
    val correlationId: UUID,
    val sourceReference: String,
    val sourceCorrelationId: UUID,
    val sourceType: SourceType,
    val gssCode: String,
    val personalDetail: PersonalDetailDto,
    val emsElectorId: String? = null,
    val historicalSearch: Boolean? = null,
    val createdBy: String,
    val createdAt: Instant? = null
)

enum class SourceType {
    VOTER_CARD,
    POSTAL_VOTE,
    PROXY_VOTE,
    OVERSEAS_VOTE,
    APPLICATIONS_API
}
