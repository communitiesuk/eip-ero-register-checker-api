package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeManagementApiException
import uk.gov.dluhc.registercheckerapi.client.IerApiException
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.config.ApiRequestErrorAttributes
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException
import javax.servlet.RequestDispatcher.ERROR_MESSAGE
import javax.servlet.RequestDispatcher.ERROR_STATUS_CODE

@ControllerAdvice
class GlobalExceptionHandler(
    private var errorAttributes: ApiRequestErrorAttributes
) : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [IerApiException::class])
    protected fun handleIerApiException(
        e: IerApiException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = INTERNAL_SERVER_ERROR
        request.setAttribute(ERROR_MESSAGE, "Error getting eroId for certificate serial", SCOPE_REQUEST)
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [IerEroNotFoundException::class])
    protected fun handleIerNotFoundApiException(
        e: IerApiException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = NOT_FOUND
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [ElectoralRegistrationOfficeManagementApiException::class])
    protected fun handleElectoralRegistrationOfficeManagementApiException(
        e: ElectoralRegistrationOfficeManagementApiException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = INTERNAL_SERVER_ERROR
        request.setAttribute(ERROR_MESSAGE, "Error retrieving GSS codes", SCOPE_REQUEST)
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [GssCodeMismatchException::class])
    protected fun handleGssCodeMismatchExceptionThrowsForbidden(
        e: GssCodeMismatchException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = FORBIDDEN
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    @ExceptionHandler(value = [RequestIdMismatchException::class])
    protected fun handleRequestIdMismatchException(
        e: RequestIdMismatchException,
        request: WebRequest
    ): ResponseEntity<Any?>? {
        val status = HttpStatus.BAD_REQUEST
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, HttpHeaders(), status, request)
    }

    override fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, headers, status, request)
    }

    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)

        return handleExceptionInternal(e, body, headers, status, request)
    }
}
