package uk.gov.dluhc.registercheckerapi.config

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn

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
        amazonSQSAsync: AmazonSQSAsync,
        objectMapper: ObjectMapper
    ): QueueMessagingTemplate =
        QueueMessagingTemplate(amazonSQSAsync, null, objectMapper)
}
