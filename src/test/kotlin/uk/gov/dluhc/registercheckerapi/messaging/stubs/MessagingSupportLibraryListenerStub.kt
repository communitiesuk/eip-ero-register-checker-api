package uk.gov.dluhc.registercheckerapi.messaging.stubs

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.MDC
import org.springframework.context.annotation.DependsOn
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.logging.config.CORRELATION_ID
import uk.gov.dluhc.logging.config.MESSAGE_ID
import uk.gov.dluhc.messagingsupport.MessageListener
import java.util.Collections
import java.util.UUID

@Component
@DependsOn("localStackContainerSqsSettings")
class MessagingSupportLibraryListenerStub : MessageListener<TestSqsMessage> {

    private val messagesReceived =
        Collections.synchronizedList(mutableListOf<SqsMessageWithMdcInfo<TestSqsMessage>>())

    @SqsListener("correlation-id-test-queue")
    override fun handleMessage(@Payload payload: TestSqsMessage) {
        messagesReceived += SqsMessageWithMdcInfo(
            payload,
            MDC.get(MESSAGE_ID),
            MDC.get(CORRELATION_ID),
        )
    }

    fun getMessages() = messagesReceived.toList()

    fun clear() {
        messagesReceived.clear()
    }
}

data class SqsMessageWithMdcInfo<T>(
    val message: T,
    val mdcMessageId: String,
    val mdcCorrelationId: String,
)

data class TestSqsMessage(
    val id: UUID = UUID.randomUUID()
)
