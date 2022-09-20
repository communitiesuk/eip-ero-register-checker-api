package uk.gov.dluhc.registercheckerapi.messaging

import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.service.InitiateRegisterCheckService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [InitiateRegisterCheckMessage] messages
 */
@Component
class InitiateRegisterCheckMessageListener(
    val initiateRegisterCheckService: InitiateRegisterCheckService
) :
    MessageListener<InitiateRegisterCheckMessage> {

    @MessageMapping("\${sqs.initiate-applicant-register-check-queue-name}")
    override fun handleMessage(@Valid @Payload payload: InitiateRegisterCheckMessage) {
        with(payload) {
            logger.info {
                "New InitiateRegisterCheckMessage received with " +
                    "sourceReference: $sourceReference and " +
                    "sourceCorrelationId: $sourceCorrelationId"
            }
            initiateRegisterCheckService.initiateRegisterCheck()
        }
    }
}
