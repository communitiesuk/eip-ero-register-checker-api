package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import java.util.UUID

fun buildRegisterCheckResultMessage(
    sourceType: RegisterCheckSourceType = RegisterCheckSourceType.VOTER_MINUS_CARD,
    sourceReference: String = "VPIOKNHPBP",
    sourceCorrelationId: UUID = UUID.randomUUID(),
    registerCheckResult: RegisterCheckResult = RegisterCheckResult.EXACT_MATCH,
    matches: List<RegisterCheckMatch> = listOf(buildRegisterCheckMatchModel())
) = RegisterCheckResultMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    registerCheckResult = registerCheckResult,
    matches = matches
)
