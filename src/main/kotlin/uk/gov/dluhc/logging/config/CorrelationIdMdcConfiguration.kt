package uk.gov.dluhc.logging.config

import org.slf4j.MDC
import java.util.UUID

/**
 * MVC Interceptor and AOP beans that set the correlation ID MDC variable for inclusion in all log statements.
 *
 * Copy of https://github.com/cabinetoffice/eip-ero-portal/blob/main/logging-lib/src/main/kotlin/uk/gov/dluhc/logging/config/CorrelationIdMdcConfiguration.kt
 */

const val CORRELATION_ID = "correlationId"
const val CORRELATION_ID_HEADER = "x-correlation-id"

fun generateCorrelationId(): String =
    UUID.randomUUID().toString().replace("-", "")

fun getCurrentCorrelationId(): String =
    MDC.get(CORRELATION_ID) ?: generateCorrelationId()
