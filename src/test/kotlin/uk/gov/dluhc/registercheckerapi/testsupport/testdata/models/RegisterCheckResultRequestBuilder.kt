package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import java.time.OffsetDateTime
import java.util.UUID

fun buildRegisterCheckResultRequest(
    requestId: UUID = UUID.randomUUID(),
    gssCode: String = "E12345678",
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    registerCheckMatchCount: Int = 0,
    registerCheckMatches: List<RegisterCheckMatch>? = emptyList()
) = RegisterCheckResultRequest(
    requestid = requestId,
    gssCode = gssCode,
    createdAt = createdAt,
    registerCheckMatchCount = registerCheckMatchCount,
    registerCheckMatches = registerCheckMatches
)
