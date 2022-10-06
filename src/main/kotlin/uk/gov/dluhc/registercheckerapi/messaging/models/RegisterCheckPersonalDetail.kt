package uk.gov.dluhc.registercheckerapi.messaging.models

import java.time.LocalDate

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
