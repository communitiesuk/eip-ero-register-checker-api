package uk.gov.dluhc.registercheckerapi.service

import org.apache.commons.text.StringSubstitutor.replace
import org.springframework.stereotype.Service
import uk.gov.dluhc.email.EmailClient
import uk.gov.dluhc.registercheckerapi.config.PendingRegisterChecksEmailContentConfiguration
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatchResultSentAtByGssCode
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckSummaryByGssCode
import java.time.temporal.ChronoUnit

@Service
class EmailService(
    private val emailClient: EmailClient,
    private val pendingRegisterChecksEmailContentConfiguration: PendingRegisterChecksEmailContentConfiguration
) {
    fun sendRegisterCheckMonitoringEmail(
        stuckRegisterCheckSummaries: List<RegisterCheckSummaryByGssCode>,
        mostRecentResponseTimesByGssCode: Map<String, RegisterCheckMatchResultSentAtByGssCode>,
        totalStuck: String,
        expectedMaximumPendingPeriod: String,
    ) {
        val pendingRegisterCheckResultsHtml = generatePendingRegisterCheckResultsHtml(
            stuckRegisterCheckSummaries,
            mostRecentResponseTimesByGssCode
        )
        val substitutionVariables = mapOf(
            "totalStuck" to totalStuck,
            "expectedMaximumPendingPeriod" to expectedMaximumPendingPeriod,
            "pendingRegisterCheckResultsHtml" to pendingRegisterCheckResultsHtml,
        )

        with(pendingRegisterChecksEmailContentConfiguration) {
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

fun generatePendingRegisterCheckResultsHtml(
    stuckRegisterCheckSummaries: List<RegisterCheckSummaryByGssCode>,
    mostRecentResponseTimesByGssCode: Map<String, RegisterCheckMatchResultSentAtByGssCode>
): String {
    return stuckRegisterCheckSummaries
        .sortedByDescending { it.registerCheckCount }
        .joinToString(separator = "\n") { summary ->
            """
                <tr>
                    <td>${summary.gssCode}</td>
                    <td>${summary.registerCheckCount}</td>
                    <td>${summary.earliestDateCreated?.truncatedTo(ChronoUnit.SECONDS)}</td>
                    <td>${mostRecentResponseTimesByGssCode[summary.gssCode]?.latestMatchResultSentAt?.truncatedTo(ChronoUnit.SECONDS) ?: "never"}</td>
                </tr>
            """.trimMargin()
        }
}
