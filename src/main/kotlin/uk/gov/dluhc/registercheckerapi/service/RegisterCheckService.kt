package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.PendingRegisterCheckNotFoundException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckUnexpectedStatusException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.RegisterCheckResultMapper
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckService(
    private val ierApiClient: IerApiClient,
    private val eroService: EroService,
    private val registerCheckRepository: RegisterCheckRepository,
    private val registerCheckRequestDataRepository: RegisterCheckResultDataRepository,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper,
    private val registerCheckResultMapper: RegisterCheckResultMapper
) {

    fun getPendingRegisterChecks(certificateSerial: String, pageSize: Int): List<PendingRegisterCheckDto> {
        val eroIdFromIer = ierApiClient.getEroIdentifier(certificateSerial).eroId!!
        return findPendingRegisterChecksByGssCodes(eroIdFromIer, pageSize)
    }

    @Transactional
    fun save(pendingRegisterCheckDto: PendingRegisterCheckDto) {
        with(pendingRegisterCheckMapper.pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)) {
            registerCheckRepository.save(this)
        }
    }

    @Transactional
    fun auditRequestBody(correlationId: UUID, requestBodyJson: String) {
        registerCheckRequestDataRepository.save(
            RegisterCheckResultData(
                correlationId = correlationId,
                requestBody = requestBodyJson
            )
        )
    }

    @Transactional
    fun updatePendingRegisterCheck(certificateSerial: String, registerCheckResultDto: RegisterCheckResultDto) {
        validateRequestIdMatch(registerCheckResultDto)
        validateGssCodeMatch(certificateSerial, registerCheckResultDto.gssCode)
        getPendingRegisterCheck(registerCheckResultDto.correlationId).apply {
            when (status) {
                CheckStatus.PENDING -> {
                    recordCheckResult(registerCheckResultDto, this)
                    // Subsequent tasks will send SQS message to VCA
                }
                else -> throw RegisterCheckUnexpectedStatusException(correlationId, status)
                    .also { logger.warn { "Register check with correlationId:[$correlationId] is in status [$status] and cannot be set to [${registerCheckResultDto.registerCheckStatus}]" } }
            }
        }
    }

    private fun recordCheckResult(registerCheckResultDto: RegisterCheckResultDto, registerCheck: RegisterCheck) {
        with(registerCheckResultDto) {
            val matches = registerCheckMatches?.map(registerCheckResultMapper::fromDtoToRegisterCheckMatchEntity) ?: emptyList()
            when (this.matchCount) {
                0 -> registerCheck.recordNoMatch(matchResultSentAt)
                1 -> registerCheck.recordExactMatch(matchResultSentAt, matches.first())
                in 2..10 -> registerCheck.recordMultipleMatches(matchResultSentAt, matchCount, matches)
                else -> registerCheck.recordTooManyMatches(matchResultSentAt, matchCount, matches)
            }
        }
    }

    private fun findPendingRegisterChecksByGssCodes(eroId: String, pageSize: Int): List<PendingRegisterCheckDto> =
        eroService.lookupGssCodesForEro(eroId).let {
            registerCheckRepository.findPendingEntriesByGssCodes(it, pageSize)
                .map(pendingRegisterCheckMapper::registerCheckEntityToPendingRegisterCheckDto)
        }

    private fun validateRequestIdMatch(registerCheckResultDto: RegisterCheckResultDto) {
        if (registerCheckResultDto.requestId != registerCheckResultDto.correlationId) {
            throw RequestIdMismatchException(registerCheckResultDto.requestId, registerCheckResultDto.correlationId)
                .also { logger.warn { it.message } }
        }
    }

    private fun validateGssCodeMatch(certificateSerial: String, requestGssCode: String) {
        val eroIdFromIer = ierApiClient.getEroIdentifier(certificateSerial).eroId!!
        if (requestGssCode !in eroService.lookupGssCodesForEro(eroIdFromIer))
            throw GssCodeMismatchException(certificateSerial, requestGssCode)
                .also { logger.warn { it.message } }
    }

    private fun getPendingRegisterCheck(correlationId: UUID): RegisterCheck =
        registerCheckRepository.findByCorrelationId(correlationId)
            ?: throw PendingRegisterCheckNotFoundException(correlationId)
                .also { logger.warn { it.message } }
}
