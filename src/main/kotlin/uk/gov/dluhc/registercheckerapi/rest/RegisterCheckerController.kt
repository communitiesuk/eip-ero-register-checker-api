package uk.gov.dluhc.registercheckerapi.rest

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckService
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
class RegisterCheckerController(
    private val registerCheckService: RegisterCheckService,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper
) {

    @GetMapping("/registerchecks")
    @PreAuthorize("isAuthenticated()")
    fun getPendingRegisterChecks(authentication: Authentication): PendingRegisterChecksResponse {
        logger.info("Getting pending register checks for EMS ERO certificateSerial=[${authentication.credentials}]")
        return registerCheckService.getPendingRegisterChecks(authentication.credentials.toString())
            .let { pendingRegisterChecks ->
                PendingRegisterChecksResponse(
                    pageSize = pendingRegisterChecks.size,
                    registerCheckRequests = pendingRegisterChecks.map(pendingRegisterCheckMapper::pendingRegisterCheckDtoToPendingRegisterCheckModel)
                )
            }
    }

	@PostMapping("/registerchecks/{requestId}")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.OK)
	fun updatePendingRegisterCheck(authentication: Authentication, @PathVariable requestId: String, @RequestBody @Valid request: RegisterCheckResultRequest) {
		logger.info("Updating pending register checks for EMS ERO certificateSerial=[${authentication.credentials}]")


	}
}
