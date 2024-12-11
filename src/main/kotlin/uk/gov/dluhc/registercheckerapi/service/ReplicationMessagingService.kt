package uk.gov.dluhc.registercheckerapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.PendingRegisterCheckArchiveMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage

@Service
class ReplicationMessagingService(
    @Qualifier("forwardInitiateRegisterCheckQueue") private val initiateCheckMessageQueue: MessageQueue<InitiateRegisterCheckMessage>,
    @Qualifier("sendRegisterCheckArchiveMessageQueue") private val archiveRegisterCheckMessageQueue: MessageQueue<PendingRegisterCheckArchiveMessage>,
    @Qualifier("forwardRemoveRegisterCheckDataMessageQueue") private val removeRegisterCheckDataMessageQueue: MessageQueue<RemoveRegisterCheckDataMessage>,
) {

    fun forwardInitiateRegisterCheckMessage(request: InitiateRegisterCheckMessage) {
        initiateCheckMessageQueue.submit(request)
    }

    fun sendArchiveRegisterCheckMessage(request: PendingRegisterCheckArchiveMessage) {
        archiveRegisterCheckMessageQueue.submit(request)
    }

    fun forwardRemoveRegisterCheckDataMessage(request: RemoveRegisterCheckDataMessage) {
        removeRegisterCheckDataMessageQueue.submit(request)
    }
}
