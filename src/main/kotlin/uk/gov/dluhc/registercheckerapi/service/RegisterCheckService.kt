package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient

private val logger = KotlinLogging.logger { }

@Service
class RegisterCheckService(private val ierApiClient: IerApiClient) {

    fun getPendingRegisterChecks(certificateSerial: String): String =
        ierApiClient.getEroIdentifier(certificateSerial).eroId!!

    fun initiateRegisterCheck() {
        logger.info { "start initiateRegisterCheck" }
        TODO("implement in next task")
    }
}
