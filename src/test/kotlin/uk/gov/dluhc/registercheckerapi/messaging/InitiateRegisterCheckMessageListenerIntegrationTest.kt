package uk.gov.dluhc.registercheckerapi.messaging

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildInitiateRegisterCheckMessage
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

internal class InitiateRegisterCheckMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val message = buildInitiateRegisterCheckMessage()
        val payload = objectMapper.writeValueAsString(message)

        // When
        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(initiateApplicantRegisterCheckQueueName)
                .messageBody(payload)
                .build()
        )

        // Then
        val stopWatch = StopWatch.createStarted()
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            verify(initiateRegisterCheckService).initiateRegisterCheck()

            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }
    }
}
