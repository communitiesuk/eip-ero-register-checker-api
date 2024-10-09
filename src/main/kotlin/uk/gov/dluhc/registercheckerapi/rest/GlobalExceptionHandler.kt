package uk.gov.dluhc.registercheckerapi.rest

import jakarta.servlet.RequestDispatcher.ERROR_MESSAGE
import jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.dluhc.registercheckerapi.client.IerApiException
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException
import uk.gov.dluhc.registercheckerapi.config.ApiRequestErrorAttributes
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.OptimisticLockingFailureException
import uk.gov.dluhc.registercheckerapi.exception.PendingRegisterCheckNotFoundException
import uk.gov.dluhc.registercheckerapi.exception.Pre1970EarliestSearchException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckMatchCountMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckUnexpectedStatusException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException

@ControllerAdvice
class GlobalExceptionHandler(
    private var errorAttributes: ApiRequestErrorAttributes,
) : ResponseEntityExceptionHandler() {
    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "Error occurred"
    }

    @ExceptionHandler(IerApiException::class)
    protected fun handleIerApiException(
        e: IerApiException,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        request.setAttribute(ERROR_MESSAGE, "Error getting eroId for certificate serial", SCOPE_REQUEST)

        return populateErrorResponseAndHandleExceptionInternal(e, INTERNAL_SERVER_ERROR, request)
    }

    @ExceptionHandler(
        value = [
            IerEroNotFoundException::class,
            PendingRegisterCheckNotFoundException::class,
        ]
    )
    protected fun handleResourceNotFound(
        e: RuntimeException,
        request: WebRequest,
    ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, NOT_FOUND, request)

    @ExceptionHandler(GssCodeMismatchException::class)
    protected fun handleGssCodeMismatchExceptionThrowsForbidden(
        e: GssCodeMismatchException,
        request: WebRequest,
    ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, FORBIDDEN, request)

    @ExceptionHandler(
        value = [
            RequestIdMismatchException::class,
            RegisterCheckMatchCountMismatchException::class,
            Pre1970EarliestSearchException::class,
        ]
    )
    protected fun handleBadRequestBusinessException(
        e: RuntimeException,
        request: WebRequest,
    ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, BAD_REQUEST, request)

    @ExceptionHandler(
        value = [
            RegisterCheckUnexpectedStatusException::class,
            OptimisticLockingFailureException::class,
        ]
    )
    fun handleExceptionReturnConflictResponse(
        e: RuntimeException,
        request: WebRequest,
    ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, CONFLICT, request)

    override fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, status, request)

    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, status, request)

    private fun populateErrorResponseAndHandleExceptionInternal(
        exception: Exception,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        request.setAttribute(ERROR_STATUS_CODE, status.value(), SCOPE_REQUEST)
        val body = errorAttributes.getErrorResponse(request)
        logger.warn(exception.message ?: DEFAULT_ERROR_MESSAGE)
        return handleExceptionInternal(exception, body, HttpHeaders(), status, request)
    }
}
