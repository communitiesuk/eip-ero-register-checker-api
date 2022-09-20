package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import java.util.UUID

@Repository
interface RegisterCheckRepository : JpaRepository<RegisterCheck, UUID>, CustomRegisterCheckRepository {

    fun findByCorrelationId(correlationId: UUID): RegisterCheck?
}
