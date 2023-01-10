package uk.gov.dluhc.registercheckerapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckRemovalDto

@Service
class RegisterCheckRemovalService(
    private val registerCheckRepository: RegisterCheckRepository,
) {

    fun remove(dto: RegisterCheckRemovalDto): String =
        TODO("Not yet implemented")
}
