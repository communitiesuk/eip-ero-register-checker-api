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
        val pendingRegisterCheckSummaries =
            registerCheckRepository.summarisePendingRegisterChecksByGssCode(createdBefore)
        val stuckRegisterCheckSummaries =
            pendingRegisterCheckSummaries.filter { !excludedGssCodes.contains(it.gssCode) }
        val totalStuck = stuckRegisterCheckSummaries.sumOf { it.registerCheckCount }

        logger.info { "A total of $totalStuck register checks have been pending for more than $expectedMaximumPendingPeriod." }
        stuckRegisterCheckSummaries.forEach {
            logger.info {
                "The gss code ${it.gssCode} has ${it.registerCheckCount} register checks " +
                    "that have been pending for more than $expectedMaximumPendingPeriod."
            }
        }

        if (sendEmail) {
            emailService.sendRegisterCheckMonitoringEmail(
                stuckRegisterCheckSummaries,
                totalStuck = totalStuck.toString(),
                expectedMaximumPendingPeriod = expectedMaximumPendingPeriod.toString(),
            )
        }
    }
}
