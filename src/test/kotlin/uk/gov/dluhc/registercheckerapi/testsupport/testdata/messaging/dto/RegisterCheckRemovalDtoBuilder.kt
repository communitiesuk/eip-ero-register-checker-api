package uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.dto

import uk.gov.dluhc.registercheckerapi.dto.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.dto.RegisterCheckRemovalDto
import java.util.UUID.randomUUID

fun buildRegisterCheckRemovalDto(
    sourceType: SourceType = SourceType.VOTER_CARD,
    sourceReference: String = randomUUID().toString(),
) = RegisterCheckRemovalDto(
    sourceType = sourceType,
    sourceReference = sourceReference,
)
