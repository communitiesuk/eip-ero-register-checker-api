package uk.gov.dluhc.registercheckerapi.database.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

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
    var dateCreated: Instant? = null
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
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated)"
    }
}
