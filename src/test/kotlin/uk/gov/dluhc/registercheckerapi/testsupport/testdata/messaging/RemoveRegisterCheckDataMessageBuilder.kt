package uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging

import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import java.util.UUID.randomUUID

fun buildRemoveRegisterCheckDataMessage(
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    sourceReference: String = randomUUID().toString(),
) = RemoveRegisterCheckDataMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
)
