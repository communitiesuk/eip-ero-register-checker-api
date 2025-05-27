package uk.gov.dluhc.registercheckerapi.testsupport

import org.assertj.core.api.Assertions.assertThat
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

class MessagingTestHelper(
    private val sqsAsyncClient: SqsAsyncClient
) {

    fun assertMessagesEnqueued(queueUrl: String, expectedNumMessages: Int) {
        val receiveMessageRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .build()
        val sqsMessages = sqsAsyncClient.receiveMessage(receiveMessageRequest)
            .get()
            .messages()
        assertThat(sqsMessages.size).isEqualTo(expectedNumMessages)
    }
}
