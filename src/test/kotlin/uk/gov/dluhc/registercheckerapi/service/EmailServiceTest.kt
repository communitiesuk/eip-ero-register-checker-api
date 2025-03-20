package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.email.EmailClient
import uk.gov.dluhc.registercheckerapi.config.PendingRegisterChecksEmailContentConfiguration
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatchResultSentAtByGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckSummaryByGssCode
import java.time.LocalDateTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
internal class EmailServiceTest {
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var emailClient: EmailClient

    companion object {
        private const val GSS_CODE_1 = "E00000001"
        private const val GSS_CODE_2 = "E00000002"
        private const val EXPECTED_TOTAL_STUCK_APPLICATIONS = "3"
        private const val EXPECTED_MAXIMUM_PENDING_PERIOD = "24 hours"
        private val DATE_CREATED_1 = LocalDateTime.of(2025, 3, 1, 10, 30).toInstant(ZoneOffset.UTC)
        private val DATE_CREATED_2 = LocalDateTime.of(2025, 3, 2, 10, 30).toInstant(ZoneOffset.UTC)
        private val MATCH_RESULT_DATE_1 = LocalDateTime.of(2025, 3, 1, 9, 30).toInstant(ZoneOffset.UTC)
        private val MATCH_RESULT_DATE_2 = LocalDateTime.of(2025, 3, 2, 9, 30).toInstant(ZoneOffset.UTC)
        private val EXPECTED_STUCK_REGISTER_CHECK_SUMMARIES = listOf(
            buildRegisterCheckSummaryByGssCode(gssCode = GSS_CODE_1, registerCheckCount = 2, earliestDateCreated = DATE_CREATED_1),
            buildRegisterCheckSummaryByGssCode(gssCode = GSS_CODE_2, registerCheckCount = 1, earliestDateCreated = DATE_CREATED_2),
        )
        private val EXPECTED_RECENT_EMS_RESPONSE_TIMES = mapOf(
            GSS_CODE_1 to buildRegisterCheckMatchResultSentAtByGssCode(gssCode = GSS_CODE_1, latestMatchResultSentAt = MATCH_RESULT_DATE_1),
            GSS_CODE_2 to buildRegisterCheckMatchResultSentAtByGssCode(gssCode = GSS_CODE_2, latestMatchResultSentAt = MATCH_RESULT_DATE_2),
        )
    }

    @Nested
    inner class SendRegisterCheckMonitoringEmail {

        @Test
        fun `should successfully send a register check monitoring email`() {
            // Given
            val expectedRecipients = setOf(
                "test1@email.com",
                "test2@email.com",
            )
            val expectedSubject = "Register Check Monitoring"

            val expectedEmailBody = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Pending register checks</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <p>A total of $EXPECTED_TOTAL_STUCK_APPLICATIONS register checks have been pending for more than $EXPECTED_MAXIMUM_PENDING_PERIOD.</p>\n" +
                "    <br>\n" +
                "    <table>\n" +
                "        <thead>\n" +
                "        <tr>\n" +
                "            <th>GSS code</th>\n" +
                "            <th>Register check count</th>\n" +
                "            <th>Date of oldest pending check</th>\n" +
                "            <th>Date of most recent successful EMS response</th>\n" +
                "        </tr>\n" +
                "        </thead>\n" +
                "        <tbody>\n" +
                "            <tr>\n" +
                "                <td>$GSS_CODE_1</td>\n" +
                "                <td>2</td>\n" +
                "                <td>2025-03-01T10:30:00Z</td>\n" +
                "                <td>2025-03-01T09:30:00Z</td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "                <td>$GSS_CODE_2</td>\n" +
                "                <td>1</td>\n" +
                "                <td>2025-03-02T10:30:00Z</td>\n" +
                "                <td>2025-03-02T09:30:00Z</td>\n" +
                "            </tr>\n" +
                "        </tbody>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>"

            // When
            val emailContentConfiguration = buildPendingRegisterChecksEmailContentConfiguration(
                expectedSubject,
                "email-templates/pending-register-checks.html",
                "test1@email.com,test2@email.com"
            )
            emailService = EmailService(emailClient, emailContentConfiguration)
            emailService.sendRegisterCheckMonitoringEmail(
                stuckRegisterCheckSummaries = EXPECTED_STUCK_REGISTER_CHECK_SUMMARIES,
                mostRecentResponseTimesByGssCode = EXPECTED_RECENT_EMS_RESPONSE_TIMES,
                totalStuck = EXPECTED_TOTAL_STUCK_APPLICATIONS,
                expectedMaximumPendingPeriod = EXPECTED_MAXIMUM_PENDING_PERIOD
            )

            // Then
            argumentCaptor<String>().apply {
                verify(emailClient).send(
                    eq(expectedRecipients),
                    eq(emptySet()),
                    eq(expectedSubject),
                    capture()
                )
                val capturedEmailBody = firstValue.filterNot { it.isWhitespace() }
                val expectedBody = expectedEmailBody.filterNot { it.isWhitespace() }

                assertThat(capturedEmailBody).matches(expectedBody)
            }

            verifyNoMoreInteractions(emailClient)
        }

        private fun buildPendingRegisterChecksEmailContentConfiguration(
            subject: String,
            emailBodyTemplate: String,
            recipients: String
        ) = PendingRegisterChecksEmailContentConfiguration(
            subject,
            emailBodyTemplate,
            recipients
        )
    }
}
