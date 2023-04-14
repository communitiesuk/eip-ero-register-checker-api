package uk.gov.dluhc.logging.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.MDC
import org.springframework.messaging.Message
import org.springframework.messaging.support.GenericMessage

/**
 * AOP Aspect to read and set the correlation ID on inbound (received) and outbound SQS [Message]s respectively.
 * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
 *
 * Copy of https://github.com/cabinetoffice/eip-ero-portal/blob/main/logging-lib/src/main/kotlin/uk/gov/dluhc/logging/config/CorrelationIdMdcMessageListenerAspect.kt
 */
@Aspect
class CorrelationIdMdcMessageListenerAspect {

    /**
     * Around Advice for inbound [Message]s (ie. SQS Message's being directed to a listener class) that sets the correlation ID
     * MDC variable to the value found in the Message header `x-correlation-id` if set, or a new value.
     * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
     */
    @Around("execution(* org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler.handleMessage(..))")
    fun aroundHandleMessage(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        val message = proceedingJoinPoint.args[0] as Message<*>?
        MDC.put(CORRELATION_ID, message?.headers?.get(CORRELATION_ID_HEADER)?.toString() ?: generateCorrelationId())
        return proceedingJoinPoint.proceed(proceedingJoinPoint.args).also {
            MDC.remove(CORRELATION_ID)
        }
    }

    /**
     * Around Advice for outbound [Message]s (ie. SQS Message's being sent) that sets the correlation ID
     * header on a new [Message] to either the existing MDC variable or a new value if not set in MDC.
     * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
     *
     * The reason this Advice is an Around is because [Message] and it's headers are immutable, so we cannot add
     * the correlation ID header on the passed [Message]. Therefore we need to create new message with the same
     * payload and a modified collection of headers.
     */
    @Around("execution(* io.awspring.cloud.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate.send(..))")
    fun aroundSendMessage(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        val queue = proceedingJoinPoint.args[0]
        val originalMessage = proceedingJoinPoint.args[1] as Message<*>
        val newMessage = GenericMessage(
            originalMessage.payload,
            originalMessage.headers.toMutableMap().plus(CORRELATION_ID_HEADER to getCurrentCorrelationId())
        )
        return proceedingJoinPoint.proceed(arrayOf(queue, newMessage))
    }
}
