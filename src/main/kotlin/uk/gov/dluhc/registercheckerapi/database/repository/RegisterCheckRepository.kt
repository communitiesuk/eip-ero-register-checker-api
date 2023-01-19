package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import java.util.UUID

@Repository
interface RegisterCheckRepository :
    JpaRepository<RegisterCheck, UUID>,
    CustomRegisterCheckRepository,
    JpaSpecificationExecutor<RegisterCheck> {

    fun findByCorrelationId(correlationId: UUID): RegisterCheck?

    fun findBySourceReferenceAndSourceType(sourceReference: String, sourceType: SourceType): List<RegisterCheck>
}
