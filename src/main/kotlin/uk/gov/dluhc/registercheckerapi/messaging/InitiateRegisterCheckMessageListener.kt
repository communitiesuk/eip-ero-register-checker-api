package uk.gov.dluhc.registercheckerapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageListener
import uk.gov.dluhc.registercheckerapi.messaging.mapper.InitiateRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckService

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [InitiateRegisterCheckMessage] messages
 */
@Component
class InitiateRegisterCheckMessageListener(
    private val registerCheckService: RegisterCheckService,
    private val mapper: InitiateRegisterCheckMapper
) :
    MessageListener<InitiateRegisterCheckMessage> {

    @SqsListener("\${sqs.initiate-applicant-register-check-queue-name}")
    override fun handleMessage(@Valid @Payload payload: InitiateRegisterCheckMessage) {
        with(payload) {
            logger.info {
                "New InitiateRegisterCheckMessage received with " +
                    "sourceReference: $sourceReference and " +
                    "sourceCorrelationId: $sourceCorrelationId"
            }
            val pendingRegisterCheckDto = mapper.initiateCheckMessageToPendingRegisterCheckDto(this)
            registerCheckService.save(pendingRegisterCheckDto)
        }
    }
}
