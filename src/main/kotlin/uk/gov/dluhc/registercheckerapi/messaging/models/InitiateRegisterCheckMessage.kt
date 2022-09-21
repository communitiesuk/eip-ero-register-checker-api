package uk.gov.dluhc.registercheckerapi.messaging.models

import java.time.LocalDate
import java.util.UUID

data class InitiateRegisterCheckMessage(
    val sourceType: RegisterCheckSourceType,
    // the VoterCardApplication.applicationId to allow the response from rca to be associated with the correct application
    val sourceReference: String,
    // the VoterCardApplicationRegisterStatus.id to allow the response from rca to be associated with the correct register status
    val sourceCorrelationId: UUID,
    // the user that requested the check or "system"
    val requestedBy: String,
    val gssCode: String,
    val personalDetail: RegisterCheckPersonalDetail,
)

data class RegisterCheckPersonalDetail(
    val firstName: String,
    val middleNames: String?,
    val surname: String,
    val dateOfBirth: LocalDate?,
    val phone: String?,
    val email: String?,
    val address: RegisterCheckAddress
)

data class RegisterCheckAddress(
    val property: String?,
    val street: String,
    val locality: String?,
    val town: String?,
    val area: String?,
    val postcode: String,
    val uprn: String?,
)

enum class RegisterCheckSourceType {
    VOTER_CARD
}
