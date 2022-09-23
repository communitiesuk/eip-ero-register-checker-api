package uk.gov.dluhc.registercheckerapi.testsupport.assertj

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterCheck
import uk.gov.dluhc.registercheckerapi.models.SourceSystem
import java.time.ZoneOffset

class PendingRegisterCheckAssert(actual: PendingRegisterCheck?) :
    AbstractAssert<PendingRegisterCheckAssert, PendingRegisterCheck?>(
        actual,
        PendingRegisterCheckAssert::class.java
    ) {

    companion object {

        private var IGNORED_FIELDS = arrayOf<String>()

        fun assertThat(actual: PendingRegisterCheck?): PendingRegisterCheckAssert {
            return PendingRegisterCheckAssert(actual)
        }
    }

    fun hasCorrectFieldsFromRegisterCheck(expected: RegisterCheck): PendingRegisterCheckAssert {
        isNotNull
        val expectedResponse = getResponseFromRegisterCheck(expected)
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*IGNORED_FIELDS)
            .ignoringCollectionOrder()
            .isEqualTo(expectedResponse)

        return this
    }

    private fun getResponseFromRegisterCheck(registerCheckEntity: RegisterCheck): PendingRegisterCheck {
        return with(registerCheckEntity) {
            PendingRegisterCheck(
                requestid = correlationId,
                source = SourceSystem.EROP,
                gssCode = gssCode,
                actingStaffId = "EROP",
                createdAt = dateCreated!!.atOffset(ZoneOffset.UTC),
                fn = personalDetail.firstName,
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
