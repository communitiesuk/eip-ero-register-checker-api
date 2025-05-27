package uk.gov.dluhc.registercheckerapi.testsupport

import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest

object ClearDownUtils {

    fun clearDownRecords(
        sqsAsyncClient: SqsAsyncClient? = null,
        queueName: String? = null
    ) {
        queueName?.let {
            val request = PurgeQueueRequest.builder().queueUrl(queueName).build()
            sqsAsyncClient?.purgeQueue(request)
        }
    }
}
