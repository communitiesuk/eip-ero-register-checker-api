package uk.gov.dluhc.registercheckerapi.service

import liquibase.repackaged.org.apache.commons.text.StringSubstitutor.replace
import org.springframework.stereotype.Service
import uk.gov.dluhc.email.EmailClient
import uk.gov.dluhc.registercheckerapi.config.EmailContentConfiguration
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckSummaryByGssCode

@Service
class EmailService(
    private val emailClient: EmailClient,
    private val emailContentConfiguration: EmailContentConfiguration
) {
    fun sendRegisterCheckMonitoringEmail(
        stuckRegisterCheckSummaries: List<RegisterCheckSummaryByGssCode>,
        totalStuck: String,
        expectedMaximumPendingPeriod: String,
    ) {
        val pendingRegisterCheckResultsHtml = generatePendingRegisterCheckResultsHtml(stuckRegisterCheckSummaries)
        val substitutionVariables = mapOf(
            "totalStuck" to totalStuck,
            "expectedMaximumPendingPeriod" to expectedMaximumPendingPeriod,
            "pendingRegisterCheckResultsHtml" to pendingRegisterCheckResultsHtml,
        )

        with(emailContentConfiguration) {
            val emailToRecipients: Set<String> = recipients.split(",").map { it.trim() }.toSet()
            val emailHtmlBody = replace(emailBody, substitutionVariables)

            emailClient.send(
                emailToRecipients = emailToRecipients,
                subject = subject,
                emailHtmlBody = emailHtmlBody
            )
        }
    }
}

fun generatePendingRegisterCheckResultsHtml(stuckRegisterCheckSummaries: List<RegisterCheckSummaryByGssCode>): String {
    return stuckRegisterCheckSummaries.joinToString(separator = "\n") { summary ->
        """
        <tr>
            <td>${summary.gssCode}</td>
            <td>${summary.registerCheckCount}</td>
        </tr>
        """.trimIndent()
    }
}
