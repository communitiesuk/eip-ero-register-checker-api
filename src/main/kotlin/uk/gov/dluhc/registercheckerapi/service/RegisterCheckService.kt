package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper

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

    fun updatePendingRegisterCheck(certificateSerial: String, registerCheckResultDto: RegisterCheckResultDto) {
        validateGssCodeMatch(certificateSerial, registerCheckResultDto.gssCode)
        // TODO update status and other logic in subsequent subtasks
    }

    private fun findPendingRegisterChecksByGssCodes(eroId: String, pageSize: Int): List<PendingRegisterCheckDto> =
        eroService.lookupGssCodesForEro(eroId).let {
            registerCheckRepository.findPendingEntriesByGssCodes(it, pageSize)
                .map(pendingRegisterCheckMapper::registerCheckEntityToPendingRegisterCheckDto)
        }

    private fun validateGssCodeMatch(certificateSerial: String, requestGssCode: String) {
        val eroIdFromIer = ierApiClient.getEroIdentifier(certificateSerial).eroId!!
        if (requestGssCode !in eroService.lookupGssCodesForEro(eroIdFromIer))
            throw GssCodeMismatchException(certificateSerial, requestGssCode)
                .also { logger.warn { "Request gssCode: [$requestGssCode] does not match with gssCode for certificateSerial: [$certificateSerial]" } }
    }
}
