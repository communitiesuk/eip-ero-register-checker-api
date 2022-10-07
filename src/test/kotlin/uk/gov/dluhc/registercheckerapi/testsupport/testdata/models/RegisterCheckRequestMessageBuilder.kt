package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import java.util.UUID

fun buildInitiateRegisterCheckMessage(
    sourceType: RegisterCheckSourceType = RegisterCheckSourceType.VOTER_CARD,
    sourceReference: String = "VPIOKNHPBP",
    sourceCorrelationId: UUID = UUID.randomUUID(),
    requestedBy: String = "system",
    gssCode: String = "E123456789",
    personalDetail: RegisterCheckPersonalDetail = buildRegisterCheckPersonalDetail()
) = InitiateRegisterCheckMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    requestedBy = requestedBy,
    gssCode = gssCode,
    personalDetail = personalDetail
)
