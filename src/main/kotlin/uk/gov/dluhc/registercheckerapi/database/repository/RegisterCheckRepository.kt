package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import java.util.UUID

@Repository
interface RegisterCheckRepository : JpaRepository<RegisterCheck, UUID> {

    @Query(value = "SELECT * FROM register_check rc WHERE rc.status = 'PENDING' AND rc.gss_code = :gssCode ORDER BY rc.date_created LIMIT :limit", nativeQuery = true)
    fun findPendingEntriesByGssCode(gssCode: String, limit: Int = 100): List<RegisterCheck>

    fun findByCorrelationId(correlationId: UUID): RegisterCheck?
}
