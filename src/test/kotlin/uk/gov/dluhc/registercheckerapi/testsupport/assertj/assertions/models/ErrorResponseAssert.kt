package uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models

import org.assertj.core.api.AbstractAssert
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import java.time.OffsetDateTime

class ErrorResponseAssert(actual: ErrorResponse?) :
    AbstractAssert<ErrorResponseAssert, ErrorResponse?>(actual, ErrorResponseAssert::class.java) {

    companion object {
        fun assertThat(actual: ErrorResponse?) = ErrorResponseAssert(actual)
    }

    fun hasTimestampNotBefore(expected: OffsetDateTime): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (timestamp.isBefore(expected)) {
                failWithMessage("Expected timestamp to not be before $expected, but was $timestamp")
            }
        }
        return this
    }

    fun hasStatus(expected: Int): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (status != expected) {
                failWithMessage("Expected status $expected, but was $status")
            }
        }
        return this
    }

    fun hasError(expected: String): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (error != expected) {
                failWithMessage("Expected error $expected, but was $error")
            }
        }
        return this
    }

    fun hasMessage(expected: String): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (message != expected) {
                failWithMessage("Expected message $expected, but was $message")
            }
        }
        return this
    }

    fun hasMessageContaining(expected: String): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (!message.contains(expected)) {
                failWithMessage("Expected message to contain $expected, but was $message")
            }
        }
        return this
    }

    fun hasValidationError(expected: String): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (validationErrors?.any { it == expected } != true) {
                failWithMessage("Expected a validation message $expected, but was $validationErrors")
            }
        }
        return this
    }

    fun hasNoValidationErrors(): ErrorResponseAssert {
        isNotNull
        with(actual!!) {
            if (validationErrors != null) {
                failWithMessage("Expected no validation messages, but was $validationErrors")
            }
        }
        return this
    }
}
