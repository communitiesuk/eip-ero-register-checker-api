package uk.gov.dluhc.registercheckerapi.messaging

import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.core.MessagePostProcessor

class MessageQueue<T : Any>(
    private val queueName: String,
    private val queueMessagingTemplate: QueueMessagingTemplate
) {
    fun submit(payload: T) =
        queueMessagingTemplate.send(queueName, queueMessagingTemplate.convertToMessage(payload))

    /**
     * Extension function on [QueueMessagingTemplate] to allow invocation of it's protected `doConvert` method to
     * get convert the payload into a [Message] with all the default headers etc.
     */
    private fun QueueMessagingTemplate.convertToMessage(payload: T): Message<T> =
        javaClass.getDeclaredMethod("doConvert", Any::class.java, Map::class.java, MessagePostProcessor::class.java).let {
            it.isAccessible = true
            it.invoke(this, payload, null, null) as Message<T>
        }
}
