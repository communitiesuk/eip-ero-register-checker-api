package uk.gov.dluhc.logging.rest

import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import uk.gov.dluhc.logging.config.CORRELATION_ID_HEADER
import uk.gov.dluhc.logging.config.getCurrentCorrelationId

/**
 * WebClient exchange filter that sets the correlation ID header to either a new value, or the
 * current value found in the MDC context. This allows for passing and logging a consistent correlation ID between
 * disparate systems or processes using the spring WebClient.
 * Example of usage:
 * ```
 *   @Configuration
 *   @Bean
 *     fun someWebclient(correlationIdExchangeFilter: CorrelationIdMdcExchangeFilter): WebClient =
 *         WebClient.builder()
 *             // Other WebClient config
 *             .filter(correlationIdExchangeFilter)
 *             .build()
 *```
 *
 * Copy of https://github.com/cabinetoffice/eip-ero-portal/blob/main/logging-lib/src/main/kotlin/uk/gov/dluhc/logging/rest/CorrelationIdWebClientMdcExchangeFilter.kt
 */
class CorrelationIdWebClientMdcExchangeFilter : ExchangeFilterFunction {

    /*
        This is modelled as a set in case we need to talk to another system within the gov space that doesn't use 'x-correlation-id'.
        Another commonly used identifier is 'X-Request-Id'. This allows us to send our 'x-correlation-id' as well as their specified one.
    */
    private val correlationHeaderNames: Set<String> = setOf(CORRELATION_ID_HEADER)

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val currentCorrelationId = getCurrentCorrelationId()
        val clientRequestModified = setCorrelationIdInRequest(request, currentCorrelationId)

        return next.filter(::mdcExchangeFilter).exchange(clientRequestModified)
    }

    private fun setCorrelationIdInRequest(request: ClientRequest, correlationId: String): ClientRequest {
        return ClientRequest.from(request)
            .headers { headers: HttpHeaders ->
                correlationHeaderNames.forEach { correlationHeaderName ->
                    headers[correlationHeaderName] = correlationId
                }
            }
            .build()
    }

    /**
     * MDC uses thread bound values. In the reactive non-blocking world, a single request could be processed by multiple
     * threads. This means that setting the MDC context at the beginning of the request is not an option. Since WebClient
     * uses reactor-netty under the hood, it runs on different threads.
     *
     * In order to continue using the MDC feature in the reactive Spring application, we need to make sure that whenever a
     * thread starts processing a request it has to update the state of the MDC context.
     */
    private fun mdcExchangeFilter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val contextMap = MDC.getCopyOfContextMap()
        return next.exchange(request).doOnEach { _ ->
            if (contextMap != null) {
                MDC.setContextMap(contextMap)
            }
        }
    }
}
