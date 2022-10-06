package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import java.util.UUID

@Repository
interface RegisterCheckRequestDataRepository : JpaRepository<RegisterCheckResultData, UUID> {

    fun findByCorrelationId(correlationId: UUID): RegisterCheckResultData?
}
