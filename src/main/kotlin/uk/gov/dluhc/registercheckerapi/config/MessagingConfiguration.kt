package uk.gov.dluhc.registercheckerapi.config

import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.listener.support.VisibilityHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.support.NotificationSubjectArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver
import org.springframework.validation.Validator
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage

@Configuration
class MessagingConfiguration {

    @Value("\${sqs.confirm-applicant-register-check-result-queue-name}")
    private lateinit var confirmRegisterCheckResultQueueName: String

    @Value("\${sqs.postal-vote-confirm-applicant-register-check-result-queue-name}")
    private lateinit var postalVoteConfirmRegisterCheckResultQueueName: String

    @Value("\${sqs.proxy-vote-confirm-applicant-register-check-result-queue-name}")
    private lateinit var proxyVoteConfirmRegisterCheckResultQueueName: String

    @Value("\${sqs.overseas-vote-confirm-applicant-register-check-result-queue-name}")
    private lateinit var overseasVoteConfirmRegisterCheckResultQueueName: String

    @Bean(name = ["confirmRegisterCheckResultQueue"])
    fun confirmRegisterCheckResultQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<RegisterCheckResultMessage>(confirmRegisterCheckResultQueueName, queueMessagingTemplate)

    @Bean(name = ["postalVoteConfirmRegisterCheckResultQueue"])
    fun postalVoteConfirmRegisterCheckResultQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<RegisterCheckResultMessage>(postalVoteConfirmRegisterCheckResultQueueName, queueMessagingTemplate)

    @Bean(name = ["proxyVoteConfirmRegisterCheckResultQueue"])
    fun proxyVoteConfirmRegisterCheckResultQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<RegisterCheckResultMessage>(proxyVoteConfirmRegisterCheckResultQueueName, queueMessagingTemplate)

    @Bean(name = ["overseasVoteConfirmRegisterCheckResultQueue"])
    fun overseasVoteConfirmRegisterCheckResultQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<RegisterCheckResultMessage>(overseasVoteConfirmRegisterCheckResultQueueName, queueMessagingTemplate)

    @Bean
    fun queueMessageHandlerFactory(
        jacksonMessageConverter: MappingJackson2MessageConverter,
        hibernateValidator: Validator
    ): QueueMessageHandlerFactory =
        QueueMessageHandlerFactory().apply {
            setArgumentResolvers(
                listOf(
                    HeadersMethodArgumentResolver(),
                    NotificationSubjectArgumentResolver(),
                    AcknowledgmentHandlerMethodArgumentResolver("Acknowledgment"),
                    VisibilityHandlerMethodArgumentResolver("Visibility"),
                    PayloadMethodArgumentResolver(jacksonMessageConverter, hibernateValidator)
                )
            )
        }
}
