package uk.gov.dluhc.registercheckerapi.messaging.models

import java.time.LocalDate

data class RegisterCheckMatch(
    val personalDetail: RegisterCheckPersonalDetail,
    val emsElectorId: String,
    val franchiseCode: String,
    val registeredStartDate: LocalDate?,
    val registeredEndDate: LocalDate?,
)
