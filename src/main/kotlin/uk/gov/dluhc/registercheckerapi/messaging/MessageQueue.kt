package uk.gov.dluhc.registercheckerapi.messaging

import io.awspring.cloud.messaging.core.QueueMessagingTemplate

class MessageQueue<T : Any>(
    private val queueName: String,
    private val queueMessagingTemplate: QueueMessagingTemplate
) {
    fun submit(message: T) =
        queueMessagingTemplate.convertAndSend(queueName, message)
}
