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
     * convert the payload into a [Message] with all the default headers etc.
     *
     * The objective is to invoke `io.awspring.cloud.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate.send(queueName, Message)`
     * via `QueueMessagingTemplate.send(queueName, Message)` so that the AOP Aspect that adds the correlation ID to the message header
     * is invoked.
     *
     * `QueueMessageTemplate.convertAndSend(queueName, payload)` (which is slightly more convenient) does not use a public method
     * on `QueueMessageTemplate` or any of its superclasses to convert the payload to a Message, so our AOP Aspect method that
     * manipulates the Message before it is sent does not get weaved or invoked.
     *
     * We need to convert the payload into a `Message` to be able to use `QueueMessageTemplate.send(queueName, Message)`, whose
     * public superclass method `io.awspring.cloud.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate.send(queueName, Message)`
     * that the Aspect weaves.
     *
     * Ordinarily we cannot call `doConvert` directly as it is `protected`. This extension function uses reflection to change the
     * `doConvert` method to public, and then invoke it and return the resultant `Message` so that we can use it in the call
     * `QueueMessageTemplate.send(queueName, Message)`
     */
    private fun QueueMessagingTemplate.convertToMessage(payload: T): Message<T> =
        javaClass.getDeclaredMethod("doConvert", Any::class.java, Map::class.java, MessagePostProcessor::class.java).let {
            it.isAccessible = true
            it.invoke(this, payload, null, null) as Message<T>
        }
}
