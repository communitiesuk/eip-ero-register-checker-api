package uk.gov.dluhc.logging.rest

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import uk.gov.dluhc.logging.config.CORRELATION_ID_HEADER
import uk.gov.dluhc.logging.config.getCurrentCorrelationId

/**
 * RestTemplate ClientHttpRequestInterceptor that sets the correlation ID header to either a new value, or the
 * current value found in the MDC context. This allows for passing and logging a consistent correlation ID between
 * disparate systems or processes using the spring RestTemplate].
 * Example of usage:
 * ```
 *   @Configuration
 *   @Bean
 *     fun someRestTemplate(correlationIdRestTemplateClientHttpRequestInterceptor: CorrelationIdRestTemplateClientHttpRequestInterceptor): RestTemplate =
 *         RestTemplateBuilder()
 *             .interceptors(correlationIdRestTemplateClientHttpRequestInterceptor)
 *             .build()
 *```
 *
 * Copy of https://github.com/cabinetoffice/eip-ero-portal/blob/main/logging-lib/src/main/kotlin/uk/gov/dluhc/logging/rest/CorrelationIdRestTemplateClientHttpRequestInterceptor.kt
 */
class CorrelationIdRestTemplateClientHttpRequestInterceptor : ClientHttpRequestInterceptor {

    /*
        This is modelled as a set in case we need to talk to another system within the gov space that doesn't use 'x-correlation-id'.
        Another commonly used identifier is 'X-Request-Id'. This allows us to send our 'x-correlation-id' as well as their specified one.
    */
    private val correlationHeaderNames: Set<String> = setOf(CORRELATION_ID_HEADER)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val correlationId = getCurrentCorrelationId()
        correlationHeaderNames.forEach { correlationHeaderName ->
            request.headers[correlationHeaderName] = correlationId
        }
        return execution.execute(request, body)
    }
}
