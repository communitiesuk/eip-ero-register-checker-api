package uk.gov.dluhc.registercheckerapi.messaging

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType.OVERSEAS_MINUS_VOTE
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType.POSTAL_MINUS_VOTE
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType.PROXY_MINUS_VOTE
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType.VOTER_MINUS_CARD

@Component
class MessageQueueResolver(
    @Qualifier("confirmRegisterCheckResultQueue") private val confirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
    @Qualifier("postalVoteConfirmRegisterCheckResultQueue") private val postalVoteConfirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
    @Qualifier("proxyVoteConfirmRegisterCheckResultQueue") private val proxyVoteConfirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
    @Qualifier("overseasVoteConfirmRegisterCheckResultQueue") private val overseasVoteConfirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
) {

    fun getTargetQueueForSourceType(sourceType: SourceType) =
        when (sourceType) {
            VOTER_MINUS_CARD -> confirmRegisterCheckResultQueue
            POSTAL_MINUS_VOTE -> postalVoteConfirmRegisterCheckResultQueue
            PROXY_MINUS_VOTE -> proxyVoteConfirmRegisterCheckResultQueue
            OVERSEAS_MINUS_VOTE -> overseasVoteConfirmRegisterCheckResultQueue
        }
}
