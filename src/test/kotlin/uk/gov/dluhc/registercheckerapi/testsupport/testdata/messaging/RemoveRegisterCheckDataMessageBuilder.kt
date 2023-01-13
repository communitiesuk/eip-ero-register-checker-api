package uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging

import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage
import java.util.UUID.randomUUID

fun buildRemoveRegisterCheckDataMessage(
    sourceType: RegisterCheckSourceType = RegisterCheckSourceType.VOTER_MINUS_CARD,
    sourceReference: String = randomUUID().toString(),
) = RemoveRegisterCheckDataMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
)
