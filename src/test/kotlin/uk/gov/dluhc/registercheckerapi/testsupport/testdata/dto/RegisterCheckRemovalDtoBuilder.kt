package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckRemovalDto
import uk.gov.dluhc.registercheckerapi.dto.SourceType
import java.util.UUID.randomUUID

fun buildRegisterCheckRemovalDto(
    sourceType: SourceType = SourceType.VOTER_CARD,
    sourceReference: String = randomUUID().toString(),
    gssCode: String = "E09000077"
) = RegisterCheckRemovalDto(
    sourceType = sourceType,
    sourceReference = sourceReference,
    gssCode = gssCode
)
