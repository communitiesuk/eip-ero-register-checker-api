package uk.gov.dluhc.registercheckerapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.messaging.stubs.MessagingSupportLibraryListenerStub
import uk.gov.dluhc.registercheckerapi.messaging.stubs.TestSqsMessage
import java.util.concurrent.TimeUnit

/**
 * Integration tests that assert that the messaging support library is correctly integrated.
 * This means message IDs and correlation IDs are correctly applied to SQS messages.
 *
 * Details testing of the inner workings are covered by the library's integration tests.
 */
internal class MessagingSupportLibraryIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var testSqsQueue: MessageQueue<TestSqsMessage>

    @Autowired
    protected lateinit var testSqsListenerStub: MessagingSupportLibraryListenerStub

    @BeforeEach
    fun setup() {
        MDC.clear()
        testSqsListenerStub.clear()
    }

    @Test
    fun `should add correlation-id header to SQS message`() {
        // When
        testSqsQueue.submit(TestSqsMessage())

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            val sqsMessages = testSqsListenerStub.getMessages()
            assertThat(sqsMessages.size).isEqualTo(1)
            val receivedSqsMessage = sqsMessages.first()
            assertThat(receivedSqsMessage.mdcCorrelationId).isNotBlank()
            assertThat(receivedSqsMessage.mdcMessageId).isNotBlank()
        }
    }
}
