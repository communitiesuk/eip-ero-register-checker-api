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
                requestid = correlationId,
                source = SourceSystem.EROP,
                gssCode = gssCode,
                actingStaffId = "EROP",
                fn = personalDetail.firstName,
                createdAt = dateCreated!!.atOffset(ZoneOffset.UTC),
                mn = personalDetail.middleNames,
                ln = personalDetail.surname,
                dob = personalDetail.dateOfBirth,
                phone = personalDetail.phoneNumber,
                email = personalDetail.email,
                regstreet = personalDetail.address.street,
                regpostcode = personalDetail.address.postcode,
                regproperty = personalDetail.address.property,
                reglocality = personalDetail.address.locality,
                regtown = personalDetail.address.town,
                regarea = personalDetail.address.area,
                reguprn = personalDetail.address.uprn
            )
        }
    }
}
