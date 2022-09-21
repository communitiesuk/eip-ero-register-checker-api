package uk.gov.dluhc.registercheckerapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedBy
import java.time.Instant
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table
@Entity
class Address(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @NotNull
    @Size(max = 255)
    var street: String,

    @Size(max = 255)
    var property: String? = null,

    @Size(max = 255)
    var locality: String? = null,

    @Size(max = 255)
    var town: String? = null,

    @Size(max = 255)
    var area: String? = null,

    @NotNull
    @Size(max = 10)
    var postcode: String,

    @Size(max = 12)
    var uprn: String? = null,

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
        other as Address

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated , createdBy = $createdBy)"
    }
}
