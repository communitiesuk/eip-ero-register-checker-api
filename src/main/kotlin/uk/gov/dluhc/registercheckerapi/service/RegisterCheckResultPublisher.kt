package uk.gov.dluhc.registercheckerapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.applicationsapi.messaging.models.SourceType.OVERSEAS_MINUS_VOTE
import uk.gov.dluhc.applicationsapi.messaging.models.SourceType.POSTAL_MINUS_VOTE
import uk.gov.dluhc.applicationsapi.messaging.models.SourceType.PROXY_MINUS_VOTE
import uk.gov.dluhc.applicationsapi.messaging.models.SourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.registercheckerapi.messaging.MessagePublisher
import uk.gov.dluhc.registercheckerapi.messaging.MessageQueue

@Service
class RegisterCheckResultPublisher(
    @Qualifier("confirmRegisterCheckResultQueue") private val confirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
    @Qualifier("postalVoteConfirmRegisterCheckResultQueue") private val postalVoteConfirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
    @Qualifier("proxyVoteConfirmRegisterCheckResultQueue") private val proxyVoteConfirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
    @Qualifier("overseasVoteConfirmRegisterCheckResultQueue") private val overseasVoteConfirmRegisterCheckResultQueue: MessageQueue<RegisterCheckResultMessage>,
) : MessagePublisher<RegisterCheckResultMessage> {

    override fun publish(payload: RegisterCheckResultMessage) {
        when (payload.sourceType) {
            VOTER_MINUS_CARD -> confirmRegisterCheckResultQueue
            POSTAL_MINUS_VOTE -> postalVoteConfirmRegisterCheckResultQueue
            PROXY_MINUS_VOTE -> proxyVoteConfirmRegisterCheckResultQueue
            OVERSEAS_MINUS_VOTE -> overseasVoteConfirmRegisterCheckResultQueue
        }.submit(payload)
    }
}
