package uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions

import mu.KotlinLogging
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import org.assertj.core.api.InstanceOfAssertFactories
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import java.time.Instant
import java.lang.Error

private val logger = KotlinLogging.logger { }

class RegisterCheckAssert(private val actualJpaEntity: RegisterCheck?) :
    AbstractAssert<RegisterCheckAssert, RegisterCheck?>(
        actualJpaEntity,
        RegisterCheckAssert::class.java
    ) {
    private var ignoringFields = arrayOf<String>()

    companion object {
        private val ID_AND_AUDIT_FIELDS = arrayOf(
            "id",
            "personalDetail.id",
            "personalDetail.address.id",
            "dateCreated",
            "personalDetail.dateCreated",
            "personalDetail.address.dateCreated",
            "updatedAt",
            "correlationId",
        )

        fun assertThat(actual: RegisterCheck?): RegisterCheckAssert {
            return RegisterCheckAssert(actual)
        }
    }

    fun isRecursivelyEqual(expectedJpaEntity: RegisterCheck?): RegisterCheckAssert {
        try {
            Assertions.assertThat(actualJpaEntity)
                .usingRecursiveComparison()
                .ignoringFields(*ignoringFields)
                .isEqualTo(expectedJpaEntity)
        } catch (e: Error) {
            logger.debug("failed to assert: ", e)
            throw e
        }
        return this
    }

    fun hasIdAndDbAuditFieldsAfter(earliestInstant: Instant) {
        Assertions.assertThat(actual!!.id!!).isNotNull
        Assertions.assertThat(actual!!.personalDetail.id!!).isNotNull
        Assertions.assertThat(actual!!.personalDetail.address.id!!).isNotNull

        Assertions.assertThat(actual!!)
            .extracting { it.dateCreated!! }
            .asInstanceOf(InstanceOfAssertFactories.INSTANT)
            .isAfter(earliestInstant)

        Assertions.assertThat(actual!!)
            .extracting { it.updatedAt!! }
            .asInstanceOf(InstanceOfAssertFactories.INSTANT)
            .isAfter(earliestInstant)
    }

    fun ignoringIdAndAuditFields(): RegisterCheckAssert {
        ignoringFields = ID_AND_AUDIT_FIELDS
        return this
    }
}
