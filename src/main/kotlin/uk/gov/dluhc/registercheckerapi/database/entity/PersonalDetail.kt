package uk.gov.dluhc.registercheckerapi.database.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table
@Entity
class PersonalDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @NotNull
    @Size(max = 255)
    var firstName: String,

    @Size(max = 255)
    var middleNames: String? = null,

    @NotNull
    @Size(max = 255)
    var surname: String,

    var dateOfBirth: LocalDate? = null,

    @Size(max = 1024)
    var email: String? = null,

    @Size(max = 50)
    var phoneNumber: String? = null,

    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @JoinColumn(name = "address_id")
    @NotFound(action = NotFoundAction.EXCEPTION)
    var address: Address,

    @Column(updatable = false)
    @CreationTimestamp
    var dateCreated: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PersonalDetail

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated)"
    }
}
