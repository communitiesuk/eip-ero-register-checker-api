package uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models

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
                createdAt = dateCreated!!.atOffset(ZoneOffset.UTC),
                emsElectorId = emsElectorId,
                historicalSearch = historicalSearch,
                fn = personalDetail.firstName,
                mn = personalDetail.middleNames,
                ln = personalDetail.surname,
                dob = personalDetail.dateOfBirth,
                regproperty = personalDetail.address.property,
                regstreet = personalDetail.address.street,
                reglocality = personalDetail.address.locality,
                regtown = personalDetail.address.town,
                regarea = personalDetail.address.area,
                regpostcode = personalDetail.address.postcode,
                reguprn = personalDetail.address.uprn,
                phone = personalDetail.phoneNumber,
                email = personalDetail.email,
            )
        }
    }
}
