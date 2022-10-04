package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun buildRegisterCheckMatch(
    id: UUID = UUID.randomUUID(),
    emsElectorId: String = UUID.randomUUID().toString(),
    attestationCount: Int = 0,
    personalDetail: PersonalDetail = buildPersonalDetail(),
    registeredStartDate: LocalDate? = LocalDate.now(),
    registeredEndDate: LocalDate? = LocalDate.now().plusDays(10),
    applicationCreatedAt: Instant = Instant.now(),
    franchiseCode: String? = ""
) = RegisterCheckMatch(
    id = id,
    emsElectorId = emsElectorId,
    attestationCount = attestationCount,
    personalDetail = personalDetail,
    registeredStartDate = registeredStartDate,
    registeredEndDate = registeredEndDate,
    applicationCreatedAt = applicationCreatedAt,
    franchiseCode = franchiseCode
)
