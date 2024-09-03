package uk.gov.dluhc.registercheckerapi.job

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.TestLogAppender
import uk.gov.dluhc.registercheckerapi.testsupport.emails.buildLocalstackEmailMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.time.Instant
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit

internal class RegisterCheckMonitoringJobIntegrationTest : IntegrationTest() {

    @Autowired
    protected lateinit var registerCheckMonitoringJob: RegisterCheckMonitoringJob

    companion object {
        private const val EXCLUDED_GSS_CODE = "E99999999"
        private const val GSS_CODE_1 = "E00000001"
        private const val GSS_CODE_2 = "E00000002"
        private const val GSS_CODE_3 = "E00000003"
        private const val EXPECTED_TOTAL_STUCK_APPLICATIONS = "3"
        private const val EXPECTED_MAXIMUM_PENDING_PERIOD = "PT24H"
        private const val EXPECTED_MAXIMUM_PENDING_HOURS = "24"

        private const val SENDERS_EMAIL_ADDRESS = "sender@domain.com"
        private const val EMAIL_SUBJECT = "Register Check Monitoring"
        private val RECIPIENTS = setOf(
            "recipient1@domain.com",
            "recipient2@domain.com"
        )

        private const val EMAIL_BODY = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Pending register checks</title>
</head>
<body>
    <p>A total of $EXPECTED_TOTAL_STUCK_APPLICATIONS register checks have been pending for more than $EXPECTED_MAXIMUM_PENDING_HOURS hours.</p>
    <br>
    <table>
        <thead>
        <tr>
            <th>GSS code</th>
            <th>Register check count</th>
        </tr>
        </thead>
        <tbody>
            <tr>
                <td>$GSS_CODE_1</td>
                <td>2</td>
            </tr>
            <tr>
                <td>$GSS_CODE_2</td>
                <td>1</td>
            </tr>
        </tbody>
    </table>
</body>
</html>
"""
    }

    @Test
    fun `should log register checks that have been in pending status for more than the set time period`() {
        // Given
        registerCheckRepository.saveAll(
            registerChecksOlderThanOneDay
        ).run {
            // This is needed to set the desired dateCreated values.
            // We need to do it after the initial save because otherwise JPA would override it.
            setDateCreatedBeforeOneDayAgo()
            registerCheckRepository.saveAll(this)
        }

        // None of these register checks should be included in the totals
        registerCheckRepository.saveAll(
            registerChecksNewerThanOneDay
        ).run {
            setDateCreatedAfterOneDayAgo()
            registerCheckRepository.saveAll(this)
        }

        // When
        registerCheckMonitoringJob.monitorPendingRegisterChecks()

        // Then
        assertThat(
            TestLogAppender.hasLog(
                "A total of $EXPECTED_TOTAL_STUCK_APPLICATIONS register checks have been pending for more than $EXPECTED_MAXIMUM_PENDING_PERIOD.",
                Level.INFO
            )
        ).isTrue
        assertThat(
            TestLogAppender.hasLog(
                "The gss code $GSS_CODE_1 has 2 register checks that have been pending for more than $EXPECTED_MAXIMUM_PENDING_PERIOD.",
                Level.INFO
            )
        ).isTrue
        assertThat(
            TestLogAppender.hasLog(
                "The gss code $GSS_CODE_2 has 1 register checks that have been pending for more than $EXPECTED_MAXIMUM_PENDING_PERIOD.",
                Level.INFO
            )
        ).isTrue
        assertThat(
            TestLogAppender.hasNoLogMatchingRegex(
                "The gss code $EXCLUDED_GSS_CODE *",
                Level.INFO
            )
        ).isTrue
        assertThat(
            TestLogAppender.hasNoLogMatchingRegex(
                "The gss code $GSS_CODE_3 *",
                Level.INFO
            )
        ).isTrue
    }

    @Test
    fun `should call email service to send pending register checks email`() {
        // Given
        registerCheckRepository.saveAll(
            registerChecksOlderThanOneDay
        ).run {
            // This is needed to set the desired dateCreated values.
            // We need to do it after the initial save because otherwise JPA would override it.
            setDateCreatedBeforeOneDayAgo()
            registerCheckRepository.saveAll(this)
        }

        // None of these register checks should be included in the totals
        registerCheckRepository.saveAll(
            registerChecksNewerThanOneDay
        ).run {
            setDateCreatedAfterOneDayAgo()
            registerCheckRepository.saveAll(this)
        }

        val expectedEmailRequest = buildLocalstackEmailMessage(
            emailSender = SENDERS_EMAIL_ADDRESS,
            toAddresses = RECIPIENTS,
            subject = EMAIL_SUBJECT,
            htmlBody = EMAIL_BODY.trimIndent(),
            timestamp = OffsetDateTime.now(UTC).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        )

        registerCheckMonitoringJob.monitorPendingRegisterChecks()

        assertEmailSent(expectedEmailRequest)
    }

    private fun List<RegisterCheck>.setDateCreatedBeforeOneDayAgo() {
        forEach { registerCheck ->
            registerCheck.dateCreated = Instant.now()
                .minus(Period.ofDays(1))
                .minusSeconds(RandomUtils.nextLong(1, 1000))
        }
    }

    private fun List<RegisterCheck>.setDateCreatedAfterOneDayAgo() {
        forEach { registerCheck ->
            registerCheck.dateCreated = Instant.now()
                .minus(Period.ofDays(1))
                .plusSeconds(RandomUtils.nextLong(1, 1000))
        }
    }

    val registerChecksOlderThanOneDay = listOf(
        // Excluded gss code should not be logged or included in the total
        buildRegisterCheck(gssCode = EXCLUDED_GSS_CODE, status = CheckStatus.PENDING),

        // Two of GSS_CODE_1's old register checks are in pending state
        buildRegisterCheck(gssCode = GSS_CODE_1, status = CheckStatus.PENDING),
        buildRegisterCheck(gssCode = GSS_CODE_1, status = CheckStatus.PENDING),
        buildRegisterCheck(gssCode = GSS_CODE_1, status = CheckStatus.EXACT_MATCH),

        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.PENDING),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.EXACT_MATCH),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.ARCHIVED),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.EXPIRED),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.NO_MATCH),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.MULTIPLE_MATCH),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.NOT_STARTED),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.PARTIAL_MATCH),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.PENDING_DETERMINATION),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.TOO_MANY_MATCHES),
    )

    val registerChecksNewerThanOneDay = listOf(
        buildRegisterCheck(gssCode = GSS_CODE_1, status = CheckStatus.PENDING),
        buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.PENDING),
        buildRegisterCheck(gssCode = GSS_CODE_3, status = CheckStatus.PENDING),
        buildRegisterCheck(gssCode = GSS_CODE_3, status = CheckStatus.EXACT_MATCH),
    )
}
