package uk.gov.dluhc.registercheckerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.mapper.PendingRegisterCheckMapper

@Service
class RegisterCheckService(
    private val ierApiClient: IerApiClient,
    private val eroService: EroService,
    private val registerCheckRepository: RegisterCheckRepository,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper
) {

    fun getPendingRegisterChecks(certificateSerial: String): List<PendingRegisterCheckDto> {
        val eroIdFromIer = ierApiClient.getEroIdentifier(certificateSerial).eroId!!
        return findPendingRegisterChecksByGssCodes(eroId = eroIdFromIer)
    }

    @Transactional
    fun save(pendingRegisterCheckDto: PendingRegisterCheckDto) {
        with(pendingRegisterCheckMapper.pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)) {
            registerCheckRepository.save(this)
        }
    }

    private fun findPendingRegisterChecksByGssCodes(eroId: String): List<PendingRegisterCheckDto> {
        val pendingRegisterChecks = mutableListOf<PendingRegisterCheckDto>()
        eroService.lookupGssCodesForEro(eroId).let { gssCodes ->
            registerCheckRepository.findPendingEntriesByGssCodes(gssCodes = gssCodes)
                .listIterator()
                .forEach { foundRegisterCheckEntity ->
                    pendingRegisterChecks.add(pendingRegisterCheckMapper.registerCheckEntityToPendingRegisterCheckDto(foundRegisterCheckEntity))
                }
        }
        return pendingRegisterChecks.toList()
    }
}
