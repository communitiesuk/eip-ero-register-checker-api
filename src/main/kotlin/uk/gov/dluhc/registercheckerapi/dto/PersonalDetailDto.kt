package uk.gov.dluhc.registercheckerapi.dto

import java.time.LocalDate

data class PersonalDetailDto(
    val firstName: String,
    val middleNames: String?,
    val surname: String,
    val dateOfBirth: LocalDate?,
    val phone: String?,
    val email: String?,
    val address: AddressDto
)
