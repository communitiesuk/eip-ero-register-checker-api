package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType.VOTER_CARD
import java.time.Instant
import java.util.UUID

fun buildRegisterCheck(
    persisted: Boolean = false,
    correlationId: UUID = UUID.randomUUID(),
    sourceReference: String = UUID.randomUUID().toString(),
    sourceCorrelationId: UUID = UUID.randomUUID(),
    sourceType: SourceType = VOTER_CARD,
    gssCode: String = "E09000021",
    status: CheckStatus = PENDING,
    matchCount: Int = 0,
    matchResultSentAt: Instant? = null,
    registerCheckMatches: MutableList<RegisterCheckMatch> = mutableListOf(),
    personalDetail: PersonalDetail = buildPersonalDetail(),
    emsElectorId: String = randomAlphanumeric(30),
    historicalSearch: Boolean = false,
    historicalSearchEarliestDate: Instant? = Instant.now(),
    createdBy: String = "system",
) = RegisterCheck(
    id = if (persisted) UUID.randomUUID() else null,
    correlationId = correlationId,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    sourceType = sourceType,
    gssCode = gssCode,
    status = status,
    matchCount = matchCount,
    matchResultSentAt = matchResultSentAt,
    registerCheckMatches = registerCheckMatches,
    personalDetail = personalDetail,
    emsElectorId = emsElectorId,
    historicalSearch = historicalSearch,
    historicalSearchEarliestDate = historicalSearchEarliestDate,
    createdBy = createdBy,
).apply {
    if (!persisted) {
        stripIdsForIntegrationTests()
    }
}

fun RegisterCheck.stripIdsForIntegrationTests() {
    id = null
    registerCheckMatches.forEach { it.stripIdsForIntegrationTests() }
}
