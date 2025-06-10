package uk.gov.dluhc.registercheckerapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.config.FeatureToggleConfiguration
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckForwardingMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.PendingRegisterCheckArchiveMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage

@Service
class ReplicationMessagingService(
    @Qualifier("forwardInitiateRegisterCheckQueue") private val initiateCheckMessageQueue: MessageQueue<InitiateRegisterCheckForwardingMessage>,
    @Qualifier("sendRegisterCheckArchiveMessageQueue") private val archiveRegisterCheckMessageQueue: MessageQueue<PendingRegisterCheckArchiveMessage>,
    @Qualifier("forwardRemoveRegisterCheckDataMessageQueue") private val removeRegisterCheckDataMessageQueue: MessageQueue<RemoveRegisterCheckDataMessage>,
    private val featureToggleConfiguration: FeatureToggleConfiguration,
) {

    fun forwardInitiateRegisterCheckMessage(request: InitiateRegisterCheckForwardingMessage) {
        if (featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding) initiateCheckMessageQueue.submit(request)
    }

    fun sendArchiveRegisterCheckMessage(request: PendingRegisterCheckArchiveMessage) {
        if (featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding) archiveRegisterCheckMessageQueue.submit(request)
    }

    fun forwardRemoveRegisterCheckDataMessage(request: RemoveRegisterCheckDataMessage) {
        if (featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding) removeRegisterCheckDataMessageQueue.submit(request)
    }
}
