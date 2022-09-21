package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType.VOTER_CARD
import java.util.UUID

fun buildRegisterCheck(
    correlationId: UUID = UUID.randomUUID(),
    sourceReference: String = UUID.randomUUID().toString(),
    sourceCorrelationId: UUID = UUID.randomUUID(),
    sourceType: SourceType = VOTER_CARD,
    gssCode: String = "E09000021",
    status: CheckStatus = PENDING,
    personalDetail: PersonalDetail = buildPersonalDetail(),
    createdBy: String = "system"
) = RegisterCheck(
    correlationId = correlationId,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    sourceType = sourceType,
    gssCode = gssCode,
    status = status,
    personalDetail = personalDetail,
    createdBy = createdBy
)
