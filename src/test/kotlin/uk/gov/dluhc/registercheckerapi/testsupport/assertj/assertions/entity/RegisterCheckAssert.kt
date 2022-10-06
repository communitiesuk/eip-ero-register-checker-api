package uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.entity

import mu.KotlinLogging
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import org.assertj.core.api.InstanceOfAssertFactories
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.toRoundedUTCOffsetDateTime
import java.time.Instant

private val logger = KotlinLogging.logger { }

class RegisterCheckAssert(private val actualJpaEntity: RegisterCheck?) :
    AbstractAssert<RegisterCheckAssert, RegisterCheck?>(
        actualJpaEntity,
        RegisterCheckAssert::class.java
    ) {
    private var ignoringFields = arrayOf<String>()

    companion object {
        private val ID_FIELDS = arrayOf(
            "id",
            "personalDetail.id",
            "personalDetail.address.id",
            "correlationId",
        )
        private val DATE_FIELDS = arrayOf(
            "dateCreated",
            "personalDetail.dateCreated",
            "personalDetail.address.dateCreated",
            "updatedAt",
            "applicationCreatedAt",
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

    fun ignoringIdFields(): RegisterCheckAssert {
        ignoringFields += ID_FIELDS
        return this
    }

    fun ignoringDateFields(): RegisterCheckAssert {
        ignoringFields += DATE_FIELDS
        return this
    }

    fun hasStatus(expected: CheckStatus): RegisterCheckAssert {
        isNotNull
        with(actual!!) {
            if (status != expected) {
                failWithMessage("Expected register check status $expected, but was $status")
            }
        }
        return this
    }

    fun hasMatchResultSentAt(expected: Instant): RegisterCheckAssert {
        isNotNull
        with(actual!!) {
            if (matchResultSentAt?.toRoundedUTCOffsetDateTime() != expected.toRoundedUTCOffsetDateTime()) {
                failWithMessage("Expected register check matchResultSentAt $expected, but was $matchResultSentAt")
            }
        }
        return this
    }

    fun hasMatchCount(expected: Int): RegisterCheckAssert {
        isNotNull
        with(actual!!) {
            if (matchCount != expected) {
                failWithMessage("Expected register check matchCount $expected, but was $matchCount")
            }
        }
        return this
    }

    fun hasRegisterCheckMatches(expected: List<RegisterCheckMatch>): RegisterCheckAssert {
        isNotNull
        with(actual!!.registerCheckMatches) {
            Assertions.assertThat(this)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringFields(*ID_FIELDS, *DATE_FIELDS)
                .isEqualTo(expected)
        }
        return this
    }
}
