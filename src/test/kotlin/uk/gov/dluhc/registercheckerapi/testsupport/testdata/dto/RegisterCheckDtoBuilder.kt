package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.SourceType
import java.time.Instant
import java.util.UUID

fun buildPendingRegisterCheckDto(
    correlationId: UUID = UUID.randomUUID(),
    sourceReference: String = UUID.randomUUID().toString(),
    sourceCorrelationId: UUID = UUID.randomUUID(),
    sourceType: SourceType = SourceType.VOTER_CARD,
    gssCode: String = "E09000021",
    personalDetail: PersonalDetailDto = buildPersonalDetailDto(),
    createdBy: String = "system",
    createdAt: Instant? = null
) = PendingRegisterCheckDto(
    correlationId = correlationId,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    sourceType = sourceType,
    gssCode = gssCode,
    personalDetail = personalDetail,
    createdBy = createdBy,
    createdAt = createdAt
)
