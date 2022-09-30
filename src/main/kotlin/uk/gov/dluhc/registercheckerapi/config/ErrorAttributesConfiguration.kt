package uk.gov.dluhc.registercheckerapi.config

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.error.ErrorAttributeOptions.Include
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.FieldError
import org.springframework.web.context.request.WebRequest
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date

@Configuration
class ErrorAttributesConfiguration {

    @Bean
    fun apiRequestErrorAttributes() = ApiRequestErrorAttributes()
}

class ApiRequestErrorAttributes : DefaultErrorAttributes() {

    companion object {
        private const val TIMESTAMP = "timestamp"
        private const val STATUS = "status"
        private const val ERROR = "error"
        private const val MESSAGE = "message"
        private const val ERRORS = "errors"

        private val errorAttributeOptions = ErrorAttributeOptions.defaults()
            .including(Include.MESSAGE, Include.BINDING_ERRORS)
    }

    fun getErrorResponse(request: WebRequest): ErrorResponse =
        getErrorAttributes(request, errorAttributeOptions).let {
            ErrorResponse(
                timestamp = it.getTimeStamp(),
                status = it.getStatus(),
                error = it.getError(),
                message = it.getMessage(),
                validationErrors = it.getErrors()
            )
        }

    private fun Map<String, Any>.getTimeStamp(): OffsetDateTime =
        (this[TIMESTAMP] as Date).toInstant().atOffset(ZoneOffset.UTC)

    private fun Map<String, Any>.getStatus(): Int =
        this[STATUS] as Int

    private fun Map<String, Any>.getError(): String =
        this[ERROR].toString()

    private fun Map<String, Any>.getMessage(): String =
        this[MESSAGE].toString()

    private fun Map<String, Any>.getErrors(): List<String>? =
        (this[ERRORS] as List<*>?)
            ?.map { it as FieldError }
            ?.map {
                "Error on field '${it.field}': rejected value [${it.rejectedValue}], ${it.defaultMessage}"
            }
}
