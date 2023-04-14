package uk.gov.dluhc.logging.rest

import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor
import uk.gov.dluhc.logging.config.CORRELATION_ID
import uk.gov.dluhc.logging.config.CORRELATION_ID_HEADER
import uk.gov.dluhc.logging.config.generateCorrelationId
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * MVC Interceptor that sets the correlation ID MDC variable of either a new value, or the value found in the
 * HTTP header `x-correlation-id` if set. This allows for passing and logging a consistent correlation ID between
 * disparate systems or processes. This interceptor is used in beans annotated with @RestController.
 *
 * Copy of https://github.com/cabinetoffice/eip-ero-portal/blob/main/logging-lib/src/main/kotlin/uk/gov/dluhc/logging/rest/CorrelationIdMdcInterceptor.kt
 */
class CorrelationIdMdcInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER) ?: generateCorrelationId()
        MDC.put(CORRELATION_ID, correlationId)
        response.setHeader(CORRELATION_ID_HEADER, correlationId)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        MDC.remove(CORRELATION_ID)
    }
}
