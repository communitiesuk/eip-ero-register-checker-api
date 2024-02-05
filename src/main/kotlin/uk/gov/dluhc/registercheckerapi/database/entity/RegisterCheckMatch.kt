package uk.gov.dluhc.registercheckerapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Table
@Entity
class RegisterCheckMatch(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @NotNull
    var emsElectorId: String,

    @NotNull
    var attestationCount: Int,

    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @JoinColumn(name = "personal_detail_id")
    @NotFound(action = NotFoundAction.EXCEPTION)
    var personalDetail: PersonalDetail,

    var registeredStartDate: LocalDate?,

    var registeredEndDate: LocalDate?,

    var applicationCreatedAt: Instant?,

    var franchiseCode: String?,

    @OneToOne(
            cascade = [CascadeType.ALL],
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )

    @JoinColumn(name = "postal_voting_arrangement", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    var postalVotingArrangement: VotingArrangement?,

    @OneToOne(
            cascade = [CascadeType.ALL],
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JoinColumn(name = "proxy_voting_arrangement", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    var proxyVotingArrangement: VotingArrangement?,

    @NotNull
    @CreationTimestamp
    var dateCreated: Instant? = null,
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
        return this::class.simpleName + "(id = $id, dateCreated = $dateCreated)"
    }
}
