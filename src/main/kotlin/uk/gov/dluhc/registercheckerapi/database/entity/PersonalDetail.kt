package uk.gov.dluhc.registercheckerapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedBy
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
import javax.validation.constraints.Size

@Table
@Entity
class PersonalDetail(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @NotNull
    @Size(max = 255)
    var firstName: String? = null,

    @Size(max = 255)
    var middleNames: String? = null,

    @NotNull
    @Size(max = 255)
    var surname: String? = null,

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
    var address: Address? = null,

    @NotNull
    @CreationTimestamp
    var dateCreated: Instant? = null,

    @NotNull
    @Size(max = 255)
    @CreatedBy
    var createdBy: String? = null
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
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated , createdBy = $createdBy)"
    }
}
