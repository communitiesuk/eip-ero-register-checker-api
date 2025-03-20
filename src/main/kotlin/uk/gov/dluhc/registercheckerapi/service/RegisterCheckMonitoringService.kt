package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckMonitoringService(
    private val registerCheckRepository: RegisterCheckRepository,
    private val emailService: EmailService,
    @Value("\${jobs.register-check-monitoring.expected-maximum-pending-period}") private val expectedMaximumPendingPeriod: Duration,
    @Value("\${jobs.register-check-monitoring.excluded-gss-codes}") private val excludedGssCodes: List<String>,
    @Value("\${jobs.register-check-monitoring.send-email}") private val sendEmail: Boolean,
) {

    @Transactional(readOnly = true)
    fun monitorPendingRegisterChecks() {
        val createdBefore = Instant.now().minus(expectedMaximumPendingPeriod)
        val stuckRegisterCheckSummaries = registerCheckRepository
            .summarisePendingRegisterChecksByGssCode(createdBefore)
            .filter { !excludedGssCodes.contains(it.gssCode) }
        val mostRecentResponseTimes = registerCheckRepository
            .findMostRecentResponseTimeForEachGssCode()
            .filter { !excludedGssCodes.contains(it.gssCode) }
            .associateBy { it.gssCode }

        val totalStuck = stuckRegisterCheckSummaries.sumOf { it.registerCheckCount }

        logger.info { "A total of $totalStuck register checks have been pending for more than $expectedMaximumPendingPeriod." }
        stuckRegisterCheckSummaries.forEach {
            logger.info {
                "The gss code ${it.gssCode} has ${it.registerCheckCount} register checks " +
                    "that have been pending for more than $expectedMaximumPendingPeriod. " +
                    "The oldest pending check has been pending since ${it.earliestDateCreated}. " +
                    "The last successful EMS response was at ${mostRecentResponseTimes[it.gssCode]?.latestMatchResultSentAt ?: "never"}."
            }
        }

        if (sendEmail) {
            val expectedMaximumPendingHours = expectedMaximumPendingPeriod.toHours().toString()

            emailService.sendRegisterCheckMonitoringEmail(
                stuckRegisterCheckSummaries,
                mostRecentResponseTimes,
                totalStuck = totalStuck.toString(),
                expectedMaximumPendingPeriod = "$expectedMaximumPendingHours hours",
            )
        }
    }
}
