package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import java.util.UUID

fun buildRegisterCheckResultMessage(
    sourceType: RegisterCheckSourceType = RegisterCheckSourceType.VOTER_CARD,
    sourceReference: String = "VPIOKNHPBP",
    sourceCorrelationId: UUID = UUID.randomUUID(),
    registerCheckResult: RegisterCheckResult = RegisterCheckResult.EXACT_MATCH,
    matches: List<RegisterCheckPersonalDetail> = listOf(buildRegisterCheckPersonalDetail())
) = RegisterCheckResultMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    registerCheckResult = registerCheckResult,
    matches = matches
)
