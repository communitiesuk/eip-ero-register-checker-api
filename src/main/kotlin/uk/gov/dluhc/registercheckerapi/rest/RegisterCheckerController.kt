package uk.gov.dluhc.registercheckerapi.rest

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.http.HttpStatus.CREATED
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.RegisterCheckResultMapper
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckService
import java.util.UUID
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@CrossOrigin
class RegisterCheckerController(
    private val registerCheckService: RegisterCheckService,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper,
    private val registerCheckResultMapper: RegisterCheckResultMapper,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val QUERY_PARAM_PAGE_SIZE = "pageSize"
    }

    @GetMapping("/registerchecks")
    @PreAuthorize("isAuthenticated()")
    fun getPendingRegisterChecks(
        authentication: Authentication,
        @RequestParam(name = QUERY_PARAM_PAGE_SIZE, required = false) pageSize: Int?
    ): PendingRegisterChecksResponse {
        logger.info("Getting pending register checks for EMS ERO certificateSerial=[${authentication.credentials}]")
        return registerCheckService
            .getPendingRegisterChecks(
                certificateSerial = authentication.credentials.toString(),
                pageSize = pageSize ?: DEFAULT_PAGE_SIZE
            ).let { pendingRegisterChecks ->
                PendingRegisterChecksResponse(
                    pageSize = pendingRegisterChecks.size,
                    registerCheckRequests = pendingRegisterChecks.map(pendingRegisterCheckMapper::pendingRegisterCheckDtoToPendingRegisterCheckModel)
                )
            }
    }

    @PostMapping("/registerchecks/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(CREATED)
    fun updatePendingRegisterCheck(
        authentication: Authentication,
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: RegisterCheckResultRequest
    ) {
        logger.info("Updating pending register checks for EMS ERO certificateSerial=[${authentication.credentials}] with requestId=[$requestId]")
        registerCheckService
            .updatePendingRegisterCheck(
                certificateSerial = authentication.credentials.toString(),
                registerCheckResultDto = registerCheckResultMapper.fromRegisterCheckResultRequestApiToDto(requestId, request),
                objectMapper.writeValueAsString(request)
            )
    }
}
