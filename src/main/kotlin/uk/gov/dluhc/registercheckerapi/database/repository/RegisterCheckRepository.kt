package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatchResultSentAtByGssCode
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckSummaryByGssCode
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import java.time.Instant
import java.util.UUID

@Repository
interface RegisterCheckRepository :
    JpaRepository<RegisterCheck, UUID>,
    CustomRegisterCheckRepository,
    JpaSpecificationExecutor<RegisterCheck> {

    fun findByCorrelationId(correlationId: UUID): RegisterCheck?

    fun findBySourceReferenceAndSourceType(sourceReference: String, sourceType: SourceType): List<RegisterCheck>

    @Query(
        """SELECT rc.gssCode AS gssCode, count(rc) as registerCheckCount, min(rc.dateCreated) as earliestDateCreated 
        FROM RegisterCheck rc
        WHERE rc.status = 'PENDING' AND rc.dateCreated < :createdBefore
        GROUP BY rc.gssCode
        """
    )
    fun summarisePendingRegisterChecksByGssCode(createdBefore: Instant): List<RegisterCheckSummaryByGssCode>

    @Query(
        """SELECT rc.gssCode as gssCode, max(rc.matchResultSentAt) as latestMatchResultSentAt 
        FROM RegisterCheck rc
        GROUP BY rc.gssCode
        """
    )
    fun findMostRecentResponseTimeForEachGssCode(): List<RegisterCheckMatchResultSentAtByGssCode>
}
