package uk.gov.dluhc.registercheckerapi.messaging.dto

import uk.gov.dluhc.registercheckerapi.dto.SourceType

data class RegisterCheckRemovalDto(
    val sourceType: SourceType,
    val sourceReference: String,
)
