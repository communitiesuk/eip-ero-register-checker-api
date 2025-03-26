package uk.gov.dluhc.registercheckerapi.database.entity

import java.time.Instant

interface RegisterCheckMatchResultSentAtByGssCode {
    val gssCode: String
    val latestMatchResultSentAt: Instant?
}
