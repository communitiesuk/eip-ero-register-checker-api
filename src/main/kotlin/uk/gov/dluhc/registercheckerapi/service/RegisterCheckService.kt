package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.EXACT_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.EXPIRED
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.NOT_STARTED
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PARTIAL_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING_DETERMINATION
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.PendingRegisterCheckNotFoundException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckUnexpectedStatusException
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.mapper.RegisterCheckResultMapper
import uk.gov.dluhc.registercheckerapi.messaging.MessageQueue
import uk.gov.dluhc.registercheckerapi.messaging.mapper.RegisterCheckResultMessageMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckService(
    private val retrieveGssCodeService: RetrieveGssCodeService,
    private val registerCheckRepository: RegisterCheckRepository,
    private val registerCheckResultDataRepository: RegisterCheckResultDataRepository,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper,
    private val registerCheckResultMapper: RegisterCheckResultMapper,
    private val registerCheckResultMessageMapper: RegisterCheckResultMessageMapper,
    private val confirmRegisterCheckResultMessageQueue: MessageQueue<RegisterCheckResultMessage>,
    private val matchStatusResolver: MatchStatusResolver
) {

    @Transactional(readOnly = true)
    fun getPendingRegisterChecks(certificateSerial: String, pageSize: Int): List<PendingRegisterCheckDto> =
        retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial).let {
            registerCheckRepository.findPendingEntriesByGssCodes(it, pageSize)
                .map(pendingRegisterCheckMapper::registerCheckEntityToPendingRegisterCheckDto)
        }

    @Transactional
    fun save(pendingRegisterCheckDto: PendingRegisterCheckDto) {
        with(pendingRegisterCheckMapper.pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)) {
            registerCheckRepository.save(this)
        }
    }

    @Transactional
    fun auditRequestBody(correlationId: UUID, requestBodyJson: String) {
        registerCheckResultDataRepository.save(
            RegisterCheckResultData(
                correlationId = correlationId,
                requestBody = requestBodyJson
            )
        )
        logger.debug { "Register check POST request payload audited/saved for requestId:[$correlationId]" }
    }

    @Transactional
    fun updatePendingRegisterCheck(certificateSerial: String, registerCheckResultDto: RegisterCheckResultDto) {
        validateGssCodeMatch(certificateSerial, registerCheckResultDto.gssCode)
        val registerCheck = getPendingRegisterCheck(registerCheckResultDto.correlationId).apply {
            when (status) {
                CheckStatus.PENDING -> recordCheckResult(registerCheckResultDto, this)
                else -> throw RegisterCheckUnexpectedStatusException(correlationId, status)
                    .also { logger.warn { "Register check with correlationId:[$correlationId] is in status [$status] and cannot be set to [${registerCheckResultDto.registerCheckStatus}]" } }
            }
        }
        with(registerCheckResultMessageMapper.fromRegisterCheckEntityToRegisterCheckResultMessage(registerCheck)) {
            logger.info {
                "Publishing ConfirmRegisterCheckResultMessage with sourceType:[$sourceType], sourceReferenceApplicationId:[$sourceReference], " +
                    "sourceCorrelationId:[$sourceCorrelationId] for correlationId:[${registerCheck.correlationId}]"
            }
            confirmRegisterCheckResultMessageQueue.submit(this)
        }
    }

    private fun recordCheckResult(registerCheckResultDto: RegisterCheckResultDto, registerCheck: RegisterCheck) {
        with(registerCheckResultDto) {
            registerCheckStatus = matchStatusResolver.resolveStatus(this, registerCheck)
            val matches = registerCheckMatches?.map(registerCheckResultMapper::fromDtoToRegisterCheckMatchEntity) ?: emptyList()
            when (registerCheckStatus!!) {
                RegisterCheckStatus.NO_MATCH -> registerCheck.recordNoMatch(matchResultSentAt)
                RegisterCheckStatus.EXACT_MATCH -> registerCheck.recordExactMatch(EXACT_MATCH, matchResultSentAt, matches.first())
                RegisterCheckStatus.PARTIAL_MATCH -> registerCheck.recordExactMatch(PARTIAL_MATCH, matchResultSentAt, matches.first())
                RegisterCheckStatus.PENDING_DETERMINATION -> registerCheck.recordExactMatch(PENDING_DETERMINATION, matchResultSentAt, matches.first())
                RegisterCheckStatus.EXPIRED -> registerCheck.recordExactMatch(EXPIRED, matchResultSentAt, matches.first())
                RegisterCheckStatus.NOT_STARTED -> registerCheck.recordExactMatch(NOT_STARTED, matchResultSentAt, matches.first())
                RegisterCheckStatus.MULTIPLE_MATCH -> registerCheck.recordMultipleMatches(matchResultSentAt, matchCount, matches)
                RegisterCheckStatus.TOO_MANY_MATCHES -> registerCheck.recordTooManyMatches(matchResultSentAt, matchCount, matches)
            }.also {
                logger.info {
                    "Updated register check status to [${registerCheck.status}], matchCount to [${registerCheck.matchCount}], " +
                        "matchResultSentAt to [${registerCheck.matchResultSentAt}] for correlationId:[${registerCheck.correlationId}]"
                }
            }
        }
    }

    fun getPendingRegisterCheck(correlationId: UUID): RegisterCheck =
        registerCheckRepository.findByCorrelationId(correlationId)
            ?: throw PendingRegisterCheckNotFoundException(correlationId)
                .also { logger.warn { it.message } }

    private fun validateGssCodeMatch(certificateSerial: String, requestGssCode: String) {
        if (requestGssCode !in retrieveGssCodeService.getGssCodeFromCertificateSerial(certificateSerial))
            throw GssCodeMismatchException(certificateSerial, requestGssCode)
                .also { logger.warn { it.message } }
    }
}
