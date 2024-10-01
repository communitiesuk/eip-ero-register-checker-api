package uk.gov.dluhc.registercheckerapi.database.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.MULTIPLE_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.NO_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.TOO_MANY_MATCHES
import java.sql.Types
import java.time.Instant
import java.util.UUID

@Table
@Entity
class RegisterCheck(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @NotNull
    @JdbcTypeCode(Types.CHAR)
    var correlationId: UUID,

    @NotNull
    @Size(max = 36)
    var sourceReference: String,

    @NotNull
    @JdbcTypeCode(Types.CHAR)
    var sourceCorrelationId: UUID,

    @NotNull
    @Size(max = 100)
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType,

    @NotNull
    @Size(max = 80)
    var gssCode: String,

    @NotNull
    @Size(max = 100)
    @Enumerated(EnumType.STRING)
    var status: CheckStatus,

    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @JoinColumn(name = "personal_detail_id")
    @NotFound(action = NotFoundAction.EXCEPTION)
    var personalDetail: PersonalDetail,

    @Size(max = 50)
    var emsElectorId: String? = null,

    var historicalSearch: Boolean? = null,

    var matchCount: Int? = null,

    var matchResultSentAt: Instant? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "register_check_id", referencedColumnName = "id", nullable = false)
    var registerCheckMatches: MutableList<RegisterCheckMatch> = mutableListOf(),

    var historicalSearchEarliestDate: Instant? = null,

    @NotNull
    @Size(max = 255)
    var createdBy: String,

    @NotNull
    @CreationTimestamp
    var dateCreated: Instant? = null,

    @NotNull
    @UpdateTimestamp
    var updatedAt: Instant? = null,

    @Version
    var version: Long = 0L,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as RegisterCheck

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , correlationId = $correlationId, dateCreated = $dateCreated , createdBy = $createdBy)"
    }

    fun recordNoMatch(matchResultSentAt: Instant) =
        recordMatchResult(NO_MATCH, 0, matchResultSentAt, emptyList(), historicalSearchEarliestDate)

    fun recordExactMatch(
        status: CheckStatus,
        matchResultSentAt: Instant,
        registerCheckMatch: RegisterCheckMatch,
        historicalSearchEarliestDate: Instant?
    ) = recordMatchResult(status, 1, matchResultSentAt, listOf(registerCheckMatch), historicalSearchEarliestDate)

    fun recordMultipleMatches(
        matchResultSentAt: Instant,
        matchCount: Int,
        registerCheckMatches: List<RegisterCheckMatch>,
        historicalSearchEarliestDate: Instant?
    ) = recordMatchResult(MULTIPLE_MATCH, matchCount, matchResultSentAt, registerCheckMatches, historicalSearchEarliestDate)

    fun recordTooManyMatches(
        matchResultSentAt: Instant,
        matchCount: Int,
        registerCheckMatches: List<RegisterCheckMatch>,
        historicalSearchEarliestDate: Instant?
    ) = recordMatchResult(TOO_MANY_MATCHES, matchCount, matchResultSentAt, registerCheckMatches, historicalSearchEarliestDate)

    private fun recordMatchResult(
        status: CheckStatus,
        matchCount: Int,
        matchResultSentAt: Instant,
        registerCheckMatches: List<RegisterCheckMatch>,
        historicalSearchEarliestDate: Instant?
    ) {
        this.status = status
        this.matchCount = matchCount
        this.matchResultSentAt = matchResultSentAt
        this.registerCheckMatches += registerCheckMatches
        this.historicalSearchEarliestDate = historicalSearchEarliestDate
    }
}

enum class SourceType {
    VOTER_CARD,
    POSTAL_VOTE,
    PROXY_VOTE,
    OVERSEAS_VOTE
}

enum class CheckStatus {
    PENDING,
    ARCHIVED, // A check can be manually archived if it is no longer needed and unable to be processed by a particular EMS
    NO_MATCH,
    EXACT_MATCH,
    PARTIAL_MATCH, // A single result from the EMS that differs slightly from our elector's details (e.g. different first name)
    MULTIPLE_MATCH,
    TOO_MANY_MATCHES,
    PENDING_DETERMINATION, // Franchise codes indicate the electors voting eligibility is still pending a decision
    EXPIRED, // elector is on the register but their registered end date is in the past
    NOT_STARTED, // elector is on the register but their registered start date is in the future
}
