package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatchResultSentAtByGssCode
import java.time.Instant

data class RegisterCheckMatchResultSentAtByGssCodeImpl(
    override val gssCode: String,
    override val latestMatchResultSentAt: Instant?
) : RegisterCheckMatchResultSentAtByGssCode

fun buildRegisterCheckMatchResultSentAtByGssCode(
    gssCode: String,
    latestMatchResultSentAt: Instant? = null
): RegisterCheckMatchResultSentAtByGssCode = RegisterCheckMatchResultSentAtByGssCodeImpl(
    gssCode = gssCode,
    latestMatchResultSentAt = latestMatchResultSentAt,
)
