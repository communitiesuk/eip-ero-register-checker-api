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
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.time.Instant
import java.time.Period

internal class RegisterCheckMonitoringJobIntegrationTest : IntegrationTest() {

    @Autowired
    protected lateinit var registerCheckMonitoringJob: RegisterCheckMonitoringJob

    companion object {
        private const val EXCLUDED_GSS_CODE = "E99999999"
        private const val GSS_CODE_1 = "E00000001"
        private const val GSS_CODE_2 = "E00000002"
        private const val GSS_CODE_3 = "E00000003"
    }

    @Test
    fun `should log register checks that have been in pending status for more than the set time period`() {
        // Given
        val registerChecksOlderThanOneDay = registerCheckRepository.saveAll(
            listOf(
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
        ).run {
            // This is needed to set the desired dateCreated values.
            // We need to do it after the initial save because otherwise JPA would override it.
            setDateCreatedBeforeOneDayAgo()
            registerCheckRepository.saveAll(this)
        }

        // None of these register checks should be included in the totals
        val registerChecksNewerThanOneDay = registerCheckRepository.saveAll(
            listOf(
                buildRegisterCheck(gssCode = GSS_CODE_1, status = CheckStatus.PENDING),
                buildRegisterCheck(gssCode = GSS_CODE_2, status = CheckStatus.PENDING),
                buildRegisterCheck(gssCode = GSS_CODE_3, status = CheckStatus.PENDING),
                buildRegisterCheck(gssCode = GSS_CODE_3, status = CheckStatus.EXACT_MATCH),
            )
        ).run {
            setDateCreatedAfterOneDayAgo()
            registerCheckRepository.saveAll(this)
        }

        // When
        registerCheckMonitoringJob.monitorPendingRegisterChecks()

        // Then
        assertThat(
            TestLogAppender.hasLog(
                "A total of 3 register checks have been pending for more than PT24H.",
                Level.INFO
            )
        ).isTrue
        assertThat(
            TestLogAppender.hasLog(
                "The gss code $GSS_CODE_1 has 2 register checks that have been pending for more than PT24H.",
                Level.INFO
            )
        ).isTrue
        assertThat(
            TestLogAppender.hasLog(
                "The gss code $GSS_CODE_2 has 1 register checks that have been pending for more than PT24H.",
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
}
