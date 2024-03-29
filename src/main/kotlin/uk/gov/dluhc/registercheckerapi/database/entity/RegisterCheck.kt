package uk.gov.dluhc.registercheckerapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.MULTIPLE_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.NO_MATCH
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.TOO_MANY_MATCHES
import java.time.Instant
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table
@Entity
class RegisterCheck(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @NotNull
    @Type(type = UUIDCharType)
    var correlationId: UUID,

    @NotNull
    @Size(max = 36)
    var sourceReference: String,

    @NotNull
    @Type(type = UUIDCharType)
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
    var version: Long? = null,
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
