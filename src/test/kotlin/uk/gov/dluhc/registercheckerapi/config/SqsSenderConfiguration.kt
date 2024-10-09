package uk.gov.dluhc.registercheckerapi.config

import io.awspring.cloud.sqs.operations.SqsTemplate
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.messagingsupport.MessagingConfigurationHelper
import uk.gov.dluhc.registercheckerapi.messaging.stubs.TestSqsMessage

@Configuration
class SqsSenderConfiguration {

    /*
     * For integration tests to function against the correct LocalStack instance,
     * it's necessary for the correct value of the property cloud.aws.sqs.endpoint to be set prior to the amazonSQS bean being created.
     * This property is set programmatically because it is determined after the LocalStack/queues have been created.
     * This happens in LocalStackContainerConfiguration.localStackContainerSettings()
     *
     * The amazonSQS spring bean is LAZILY created in  io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration.SqsClientConfiguration.amazonSQS()
     *
     * This ordering is achieved implicitly by the @DependsOn annotation below.
     */
    @Bean
    @DependsOn("localStackContainerSqsSettings")
    fun sqsMessagingTemplate(
        sqsAsyncClient: SqsAsyncClient,
        sqsMessagingMessageConverter: SqsMessagingMessageConverter,
    ): SqsTemplate = MessagingConfigurationHelper.sqsTemplate(sqsAsyncClient, sqsMessagingMessageConverter)

    @Bean
    fun testSqsQueue(
        sqsTemplate: SqsTemplate,
        @Value("correlation-id-test-queue") queueName: String,
    ) = MessageQueue<TestSqsMessage>(queueName, sqsTemplate)
}
