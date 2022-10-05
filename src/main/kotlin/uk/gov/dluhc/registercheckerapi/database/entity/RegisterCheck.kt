package uk.gov.dluhc.registercheckerapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.EXACT_MATCH
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

    var matchCount: Int? = null,

    var matchResultSentAt: Instant? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "correlation_id", nullable = false)
    var registerCheckMatches: MutableList<RegisterCheckMatch> = mutableListOf(),

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

    fun recordMatchResult(
        matchCount: Int,
        matchResultSentAt: Instant,
        registerCheckMatches: List<RegisterCheckMatch>
    ) {
        when (matchCount) {
            0 -> recordNoMatch(matchResultSentAt)
            1 -> recordExactMatch(matchResultSentAt, registerCheckMatches.first())
            in 2..10 -> recordMultipleMatches(matchResultSentAt, matchCount, registerCheckMatches)
            else -> recordTooManyMatches(matchResultSentAt, matchCount, registerCheckMatches)
        }
    }

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

    private fun recordNoMatch(matchResultSentAt: Instant) =
        recordMatchResult(NO_MATCH, 0, matchResultSentAt, emptyList())

    private fun recordExactMatch(matchResultSentAt: Instant, registerCheckMatch: RegisterCheckMatch) =
        recordMatchResult(EXACT_MATCH, 1, matchResultSentAt, listOf(registerCheckMatch))

    private fun recordMultipleMatches(matchResultSentAt: Instant, matchCount: Int, registerCheckMatches: List<RegisterCheckMatch>) =
        recordMatchResult(MULTIPLE_MATCH, matchCount, matchResultSentAt, registerCheckMatches)

    private fun recordTooManyMatches(matchResultSentAt: Instant, matchCount: Int, registerCheckMatches: List<RegisterCheckMatch>) =
        recordMatchResult(TOO_MANY_MATCHES, matchCount, matchResultSentAt, registerCheckMatches)

    private fun recordMatchResult(
        status: CheckStatus,
        matchCount: Int,
        matchResultSentAt: Instant,
        registerCheckMatches: List<RegisterCheckMatch>
    ) {
        this.status = status
        this.matchCount = matchCount
        this.matchResultSentAt = matchResultSentAt
        this.registerCheckMatches += registerCheckMatches
    }
}

enum class SourceType {
    VOTER_CARD
}

enum class CheckStatus {
    PENDING,
    NO_MATCH,
    EXACT_MATCH,
    MULTIPLE_MATCH,
    TOO_MANY_MATCHES
}
