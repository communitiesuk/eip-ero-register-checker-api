package uk.gov.dluhc.registercheckerapi.job

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckSummaryByGssCode
import uk.gov.dluhc.registercheckerapi.service.EmailService
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckMonitoringService
import uk.gov.dluhc.registercheckerapi.testsupport.TestLogAppender
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckSummaryByGssCode
import java.time.Duration
import java.time.Instant
import java.time.Period

internal class RegisterCheckMonitoringJobIntegrationTest : IntegrationTest() {

    @Autowired
    protected lateinit var registerCheckMonitoringJob: RegisterCheckMonitoringJob

    @Mock
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setupMocks() {
        val service = RegisterCheckMonitoringService(
            registerCheckRepository,
            emailService,
            Duration.ofHours(24),
            listOf(EXCLUDED_GSS_CODE),
            true
        )
        registerCheckMonitoringJob = RegisterCheckMonitoringJob(service)
    }

    companion object {
        private const val EXCLUDED_GSS_CODE = "E99999999"
        private const val GSS_CODE_1 = "E00000001"
        private const val GSS_CODE_2 = "E00000002"
        private const val GSS_CODE_3 = "E00000003"
        private const val EXPECTED_TOTAL_STUCK_APPLICATIONS = "3"
        private const val EXPECTED_MAXIMUM_PENDING_PERIOD = "PT24H"
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

        Mockito.doNothing().`when`(emailService).sendRegisterCheckMonitoringEmail(any(), any(), any())

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

        val captor = argumentCaptor<List<RegisterCheckSummaryByGssCode>>()
        Mockito.doNothing().`when`(emailService).sendRegisterCheckMonitoringEmail(any(), any(), any())

        // When
        registerCheckMonitoringJob.monitorPendingRegisterChecks()

        // Assert
        verify(emailService).sendRegisterCheckMonitoringEmail(
            captor.capture(),
            eq(EXPECTED_TOTAL_STUCK_APPLICATIONS),
            eq(EXPECTED_MAXIMUM_PENDING_PERIOD)
        )

        val actualPendingChecks = captor.firstValue
        assertThat(actualPendingChecks)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedPendingChecks)
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

    val expectedPendingChecks: List<RegisterCheckSummaryByGssCode> = listOf(
        buildRegisterCheckSummaryByGssCode(gssCode = GSS_CODE_1, registerCheckCount = 2),
        buildRegisterCheckSummaryByGssCode(gssCode = GSS_CODE_2, registerCheckCount = 1),
    )
}
