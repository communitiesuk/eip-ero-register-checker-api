package uk.gov.dluhc.registercheckerapi.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus.CREATED
import org.springframework.orm.ObjectOptimisticLockingFailureException
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
import uk.gov.dluhc.registercheckerapi.exception.OptimisticLockingFailureException
import uk.gov.dluhc.registercheckerapi.mapper.AdminPendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.RegisterCheckResultMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.PendingRegisterCheckArchiveMessage
import uk.gov.dluhc.registercheckerapi.models.AdminPendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckService
import uk.gov.dluhc.registercheckerapi.service.ReplicationMessagingService
import uk.gov.dluhc.registercheckerapi.validator.RegisterCheckRequestValidator
import java.util.UUID

private val logger = KotlinLogging.logger {}
private const val DEFAULT_PAGE_SIZE = 100
private const val QUERY_PARAM_PAGE_SIZE = "pageSize"

@RestController
@CrossOrigin
class RegisterCheckerController(
    private val registerCheckService: RegisterCheckService,
    private val registerCheckRequestValidator: RegisterCheckRequestValidator,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper,
    private val adminPendingRegisterCheckMapper: AdminPendingRegisterCheckMapper,
    private val registerCheckResultMapper: RegisterCheckResultMapper,
    private val objectMapper: ObjectMapper,
    private val replicationMessagingService: ReplicationMessagingService,
) {

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

    @GetMapping("/admin/pending-checks/{eroId}")
    fun adminGetPendingRegisterChecks(
        @PathVariable eroId: String
    ): AdminPendingRegisterChecksResponse {
        logger.info("Getting admin pending register checks for eroId=[$eroId]")
        return AdminPendingRegisterChecksResponse(
            pendingRegisterChecks = registerCheckService.adminGetPendingRegisterChecks(eroId).map(
                adminPendingRegisterCheckMapper::adminPendingRegisterCheckDtoToAdminPendingRegisterCheckModel
            )
        )
    }

    @PostMapping("/registerchecks/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(CREATED)
    fun updatePendingRegisterCheck(
        authentication: Authentication,
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: RegisterCheckResultRequest
    ) {
        val certificateSerial = authentication.credentials.toString()
        logger.info("Updating pending register checks for EMS certificateSerial=[$certificateSerial] with requestId=[$requestId]")

        registerCheckService.auditRequestBody(request.requestid, objectMapper.writeValueAsString(request))

        val registerCheckResultDto = registerCheckResultMapper.fromRegisterCheckResultRequestApiToDto(requestId, request)
        registerCheckRequestValidator.validateRequestBody(certificateSerial, registerCheckResultDto)

        logger.debug("Post request body validation successful for EMS certificateSerial=[$certificateSerial] with requestId=[$requestId]")

        try {
            val registerCheck = registerCheckService.updatePendingRegisterCheck(certificateSerial, registerCheckResultDto)
            registerCheckService.sendConfirmRegisterCheckResultMessage(registerCheck)
            val pendingRegisterCheckArchiveMessage = PendingRegisterCheckArchiveMessage(registerCheck.correlationId)
            replicationMessagingService.sendArchiveRegisterCheckMessage(pendingRegisterCheckArchiveMessage)
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw (OptimisticLockingFailureException(registerCheckResultDto.correlationId)).also {
                logger.warn { "Register check with correlationId:[${registerCheckResultDto.correlationId}] had an optimistic locking failure" }
            }
        }
    }
}
