package uk.gov.dluhc.registercheckerapi.messaging.models

import java.util.UUID

data class RegisterCheckResultMessage(
    val sourceType: RegisterCheckSourceType,
    // the VoterCardApplication.applicationId to allow the response from rca to be associated with the correct application
    val sourceReference: String,
    // the VoterCardApplicationRegisterStatus.id to allow the response from rca to be associated with the correct register status
    val sourceCorrelationId: UUID,
    val registerCheckResult: RegisterCheckResult,
    val matches: List<RegisterCheckPersonalDetail>,
)

enum class RegisterCheckResult {
    EXACT_MATCH,
    NO_MATCH,
    MULTIPLE_MATCH,
    TOO_MANY_MATCHES,
}
