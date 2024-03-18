package uk.gov.dluhc.registercheckerapi.testsupport

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.util.concurrent.CopyOnWriteArrayList

class TestLogAppender : AppenderBase<ILoggingEvent>() {
    override fun append(loggingEvent: ILoggingEvent) {
        logList.add(loggingEvent)
    }

    companion object {
        private val logList: MutableList<ILoggingEvent> = CopyOnWriteArrayList()
        val logs: MutableList<ILoggingEvent> get() = logList

        fun hasLog(message: String, level: Level): Boolean =
            logList.firstOrNull { hasMessage(it, message, level) } != null

        fun hasLogMatchingRegex(pattern: String, level: Level): Boolean =
            logList.firstOrNull { hasMessageMatchingRegex(it, Regex(pattern), level) } != null

        fun hasNoLogMatchingRegex(pattern: String, level: Level): Boolean =
            logList.firstOrNull { hasMessageMatchingRegex(it, Regex(pattern), level) } == null

        fun getLogEventMatchingRegex(pattern: String, level: Level): ILoggingEvent? =
            logList.firstOrNull { hasMessageMatchingRegex(it, Regex(pattern), level) }

        private fun hasMessage(event: ILoggingEvent, message: String, level: Level): Boolean {
            val throwableProxy = event.throwableProxy
            return (
                message == event.formattedMessage || (throwableProxy != null && message == throwableProxy.message)
                ) && event.level == level
        }

        private fun hasMessageMatchingRegex(event: ILoggingEvent, regex: Regex, level: Level): Boolean {
            val throwableProxy = event.throwableProxy
            return (
                regex.matches(event.formattedMessage) ||
                    (throwableProxy != null && throwableProxy.message != null && regex.matches(throwableProxy.message))
                ) && event.level == level
        }

        fun reset() {
            logList.clear()
        }
    }
}
