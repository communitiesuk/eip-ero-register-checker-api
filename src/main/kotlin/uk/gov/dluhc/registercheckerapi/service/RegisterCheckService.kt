package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckNotFoundException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckService(
    private val ierApiClient: IerApiClient,
    private val eroService: EroService,
    private val registerCheckRepository: RegisterCheckRepository,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper
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
    fun updatePendingRegisterCheck(certificateSerial: String, registerCheckResultDto: RegisterCheckResultDto) {
        validateRequestIdMatch(registerCheckResultDto)
        validateGssCodeMatch(certificateSerial, registerCheckResultDto.gssCode)
        getPendingRegisterCheck(registerCheckResultDto.correlationId)
        // TODO update status and persist RegisterCheckMatchDto payload in subsequent subtasks
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
            ?: throw RegisterCheckNotFoundException(correlationId)
                .also { logger.warn { it.message } }
}
