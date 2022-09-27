package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import java.time.OffsetDateTime
import java.util.UUID

fun buildRegisterCheckResultRequest(
    requestId: UUID = UUID.randomUUID(),
    gssCode: String = "E12345678",
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    registerCheckMatchCount: Int = 1
) = RegisterCheckResultRequest(
    requestid = requestId,
    gssCode = gssCode,
    createdAt = createdAt,
    registerCheckMatchCount = registerCheckMatchCount
)
