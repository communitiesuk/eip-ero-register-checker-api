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
    private val registerCheckRepository: RegisterCheckRepository,
    private val pendingRegisterCheckMapper: PendingRegisterCheckMapper,
) {

    fun getPendingRegisterChecks(certificateSerial: String): String =
        ierApiClient.getEroIdentifier(certificateSerial).eroId!!

    @Transactional
    fun save(pendingRegisterCheckDto: PendingRegisterCheckDto) {
        with(pendingRegisterCheckMapper.pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)) {
            registerCheckRepository.save(this)
        }
    }
}
