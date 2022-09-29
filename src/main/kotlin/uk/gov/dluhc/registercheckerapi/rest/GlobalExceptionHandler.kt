package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeManagementApiException
import uk.gov.dluhc.registercheckerapi.client.IerApiException
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import java.time.ZoneOffset
import java.util.Date
import javax.servlet.RequestDispatcher

@ControllerAdvice
class GlobalExceptionHandler(private var errorAttributes: ErrorAttributes) : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [IerApiException::class])
    protected fun handleIerApiException(
        e: IerApiException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = INTERNAL_SERVER_ERROR
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Error getting eroId for certificate serial", RequestAttributes.SCOPE_REQUEST)
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [IerEroNotFoundException::class])
    protected fun handleIerNotFoundApiException(
        e: IerApiException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = NOT_FOUND
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [ElectoralRegistrationOfficeManagementApiException::class])
    protected fun handleElectoralRegistrationOfficeManagementApiException(
        e: ElectoralRegistrationOfficeManagementApiException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = INTERNAL_SERVER_ERROR
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Error retrieving GSS codes", RequestAttributes.SCOPE_REQUEST)
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [GssCodeMismatchException::class])
    protected fun handleGssCodeMismatchExceptionThrowsForbidden(
        e: GssCodeMismatchException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = FORBIDDEN
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    override fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = getErrorResponse(request)

        return handleExceptionInternal(e, body, headers, status, request)
    }

    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
        val body = getErrorResponse(request)

        return handleExceptionInternal(e, body, headers, status, request)
    }

    private fun getErrorResponse(request: WebRequest): ErrorResponse {
        val errorAttributes = getErrorAttributes(request)
        return ErrorResponse(
            timestamp = (errorAttributes.get("timestamp") as Date).toInstant().atOffset(ZoneOffset.UTC),
            status = errorAttributes.get("status") as Int,
            error = errorAttributes.get("error") as String,
            message = errorAttributes.get("message") as String,
            validationErrors = (errorAttributes.get("errors") as List<FieldError>?)?.map {
                "Error on field '${it.field}': rejected value [${it.rejectedValue}], ${it.defaultMessage}"
            }
        )
    }

    private fun getErrorAttributes(request: WebRequest): Map<String, Any> {
        val errorAttributeOptions = ErrorAttributeOptions.defaults()
            .including(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.BINDING_ERRORS)
        return this.errorAttributes.getErrorAttributes(request, errorAttributeOptions)
    }
}
