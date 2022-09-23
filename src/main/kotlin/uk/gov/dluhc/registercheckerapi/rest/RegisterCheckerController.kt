package uk.gov.dluhc.registercheckerapi.rest

import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckService

private val logger = KotlinLogging.logger {}

@RestController
class RegisterCheckerController(private val registerCheckService: RegisterCheckService) {

    @GetMapping("/registerchecks")
    @PreAuthorize("isAuthenticated()")
    fun getPendingRegisterChecks(authentication: Authentication): PendingRegisterChecksResponse {
        logger.info("Getting pending register checks for EMS ERO certificateSerial=[${authentication.credentials}]")
        return registerCheckService.getPendingRegisterChecks(certificateSerial = authentication.credentials.toString())
            .let { pendingRegisterCheckDto ->
                PendingRegisterChecksResponse(
                    pageSize = pendingRegisterCheckDto.size,
                    registerCheckRequests = emptyList() // FIXME this will return the mapper api model in next subtask EIP1-1859
                )
            }
    }
}
