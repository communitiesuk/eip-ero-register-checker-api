package uk.gov.dluhc.registercheckerapi.testsupport.assertj

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterCheck
import uk.gov.dluhc.registercheckerapi.models.SourceSystem
import java.time.ZoneOffset

class PendingRegisterCheckAssert(actual: List<PendingRegisterCheck>?) :
    AbstractAssert<PendingRegisterCheckAssert, List<PendingRegisterCheck>?>(
        actual,
        PendingRegisterCheckAssert::class.java
    ) {

    companion object {

        private val IGNORED_FIELDS = arrayOf(
            "createdAt"
        )

        fun assertThat(actual: List<PendingRegisterCheck>?): PendingRegisterCheckAssert {
            return PendingRegisterCheckAssert(actual)
        }
    }

    fun hasPendingRegisterChecksInOrder(expected: List<RegisterCheck>): PendingRegisterCheckAssert {
        isNotNull
        val expectedPendingRegisterChecks = expected.map {
            toPendingRegisterCheckFromEntity(it)
        }
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*IGNORED_FIELDS)
            .isEqualTo(expectedPendingRegisterChecks)

        return this
    }

    fun hasEmptyPendingRegisterChecks(): PendingRegisterCheckAssert {
        isNotNull
        Assertions.assertThat(actual)
            .isNotNull
            .isEmpty()

        return this
    }

    private fun toPendingRegisterCheckFromEntity(registerCheckEntity: RegisterCheck): PendingRegisterCheck {
        return with(registerCheckEntity) {
            PendingRegisterCheck(
                requestid = this.correlationId,
                source = SourceSystem.EROP,
                gssCode = this.gssCode,
                actingStaffId = "EROP",
                fn = this.personalDetail.firstName,
                createdAt = this.dateCreated!!.atOffset(ZoneOffset.UTC),
                mn = this.personalDetail.middleNames,
                ln = this.personalDetail.surname,
                dob = this.personalDetail.dateOfBirth,
                phone = this.personalDetail.phoneNumber,
                email = this.personalDetail.email,
                regstreet = this.personalDetail.address.street,
                regpostcode = this.personalDetail.address.postcode,
                regproperty = this.personalDetail.address.property,
                reglocality = this.personalDetail.address.locality,
                regtown = this.personalDetail.address.town,
                regarea = this.personalDetail.address.area,
                reguprn = this.personalDetail.address.uprn
            )
        }
    }
}
