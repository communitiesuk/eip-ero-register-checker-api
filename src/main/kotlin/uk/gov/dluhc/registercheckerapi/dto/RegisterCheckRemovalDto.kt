package uk.gov.dluhc.registercheckerapi.dto

data class RegisterCheckRemovalDto(
    val sourceType: SourceType,
    val sourceReference: String,
    val gssCode: String,
)
