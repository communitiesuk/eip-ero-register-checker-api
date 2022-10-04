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
import java.util.Collections.singletonList
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

    fun recordNoMatch(matchResultSentAt: Instant): RegisterCheck {
        this.status = NO_MATCH
        this.matchCount = 0
        this.matchResultSentAt = matchResultSentAt
        return this
    }

    fun recordExactMatch(matchResultSentAt: Instant, registerCheckMatch: RegisterCheckMatch): RegisterCheck {
        this.status = EXACT_MATCH
        this.matchCount = 1
        this.matchResultSentAt = matchResultSentAt
        return addRegisterCheckMatches(singletonList(registerCheckMatch))
    }

    fun recordMultipleMatches(matchResultSentAt: Instant, registerCheckMatches: List<RegisterCheckMatch>): RegisterCheck {
        this.status = MULTIPLE_MATCH
        this.matchCount = registerCheckMatches.size
        this.matchResultSentAt = matchResultSentAt
        return addRegisterCheckMatches(registerCheckMatches)
    }

    fun recordTooManyMatches(matchResultSentAt: Instant, registerCheckMatches: List<RegisterCheckMatch>): RegisterCheck {
        this.status = TOO_MANY_MATCHES
        this.matchCount = registerCheckMatches.size
        this.matchResultSentAt = matchResultSentAt
        return addRegisterCheckMatches(registerCheckMatches)
    }

    private fun addRegisterCheckMatches(registerCheckMatches: List<RegisterCheckMatch>): RegisterCheck {
        this.registerCheckMatches += registerCheckMatches
        return this
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
