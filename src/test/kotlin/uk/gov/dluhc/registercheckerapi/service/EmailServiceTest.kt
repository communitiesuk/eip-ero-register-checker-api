package uk.gov.dluhc.votercardapplicationsapi.service

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
import uk.gov.dluhc.emailnotifications.SesEmailClient
import uk.gov.dluhc.registercheckerapi.config.EmailContentConfiguration
import uk.gov.dluhc.registercheckerapi.service.EmailService
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckSummaryByGssCode

@ExtendWith(MockitoExtension::class)
internal class EmailServiceTest {
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var sesEmailClient: SesEmailClient

    companion object {
        private const val GSS_CODE_1 = "E00000001"
        private const val GSS_CODE_2 = "E00000002"
        private const val EXPECTED_TOTAL_STUCK_APPLICATIONS = "3"
        private const val EXPECTED_MAXIMUM_PENDING_PERIOD = "PT24H"
        private val EXPECTED_STUCK_REGISTER_CHECK_SUMMARIES = listOf(
            buildRegisterCheckSummaryByGssCode(gssCode = GSS_CODE_1, registerCheckCount = 2),
            buildRegisterCheckSummaryByGssCode(gssCode = GSS_CODE_2, registerCheckCount = 1),
        )
    }

    @Nested
    inner class SendRegisterCheckMonitoringEmail {

        @Test
        fun `should successfully send a register check monitoring email`() {
            // Given
            val expectedRecipients = setOf("test@email.com")
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
                "        </tr>\n" +
                "        </thead>\n" +
                "        <tbody>\n" +
                "            <tr>\n" +
                "                <td>$GSS_CODE_1</td>\n" +
                "                <td>2</td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "                <td>$GSS_CODE_2</td>\n" +
                "                <td>1</td>\n" +
                "            </tr>\n" +
                "        </tbody>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>"

            // When
            val emailContentConfiguration = buildEmailContentConfiguration(
                expectedSubject,
                "email-templates/pending-register-checks.html",
                "test@email.com"
            )
            emailService = EmailService(sesEmailClient, emailContentConfiguration)
            emailService.sendRegisterCheckMonitoringEmail(
                stuckRegisterCheckSummaries = EXPECTED_STUCK_REGISTER_CHECK_SUMMARIES,
                totalStuck = EXPECTED_TOTAL_STUCK_APPLICATIONS,
                expectedMaximumPendingPeriod = EXPECTED_MAXIMUM_PENDING_PERIOD
            )

            // Then
            argumentCaptor<String>().apply {
                verify(sesEmailClient).send(
                    eq(expectedRecipients),
                    eq(emptySet()),
                    eq(expectedSubject),
                    capture()
                )
                val capturedEmailBody = firstValue.filterNot { it.isWhitespace() }
                val expectedBody = expectedEmailBody.filterNot { it.isWhitespace() }

                assertThat(capturedEmailBody).matches(expectedBody)
            }

            verifyNoMoreInteractions(sesEmailClient)
        }

        private fun buildEmailContentConfiguration(
            subject: String,
            emailBodyTemplate: String,
            recipients: String
        ) = EmailContentConfiguration(
            subject,
            emailBodyTemplate,
            recipients
        )
    }
}
