package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType.VOTER_CARD
import java.util.UUID

fun buildRegisterCheck(
    id: UUID = UUID.randomUUID(),
    correlationId: UUID = UUID.randomUUID(),
    sourceReference: String = UUID.randomUUID().toString(),
    sourceCorrelationId: UUID = UUID.randomUUID(),
    sourceType: SourceType = VOTER_CARD,
    gssCode: String = "E09000021",
    status: CheckStatus = PENDING,
    matchCount: Int = 0,
    registerCheckMatches: MutableList<RegisterCheckMatch> = mutableListOf(),
    personalDetail: PersonalDetail = buildPersonalDetail(),
    emsElectorId: String = randomAlphanumeric(30),
    historicalSearch: Boolean = false,
    createdBy: String = "system"
) = RegisterCheck(
    id = id,
    correlationId = correlationId,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    sourceType = sourceType,
    gssCode = gssCode,
    status = status,
    matchCount = matchCount,
    registerCheckMatches = registerCheckMatches,
    personalDetail = personalDetail,
    emsElectorId = emsElectorId,
    historicalSearch = historicalSearch,
    createdBy = createdBy
)
