package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

@Service
class InitiateRegisterCheckService {
    fun initiateRegisterCheck() {
        logger.info { "start initiateRegisterCheck" }
        TODO("implement in next task")
    }
}
