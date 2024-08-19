package uk.gov.dluhc.emailnotifications

import mu.KotlinLogging
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SesException

private val logger = KotlinLogging.logger {}

class SesEmailClient(
    private val sesClient: SesClient,
    private val sender: String
) : EmailClient {

    override fun send(emailToRecipients: Set<String>, emailCcRecipients: Set<String>, subject: String, emailHtmlBody: String): Set<String> {
        if (emailToRecipients.isEmpty() && emailCcRecipients.isEmpty()) {
            throw EmailNotSentException("Failed to send email due to recipientsToEmail and recipientsToCc being empty")
        }

        val emailRequest: SendEmailRequest = buildSendEmailRequest(
            sender = sender,
            emailToAddresses = emailToRecipients,
            emailCcAddresses = emailCcRecipients,
            subject = subject,
            emailHtmlBody = emailHtmlBody,
        )

        try {
            logger.debug { "sending an email to [$emailToRecipients], cc[$emailCcRecipients] with subject[$subject]" }
            val messageId = sesClient.sendEmail(emailRequest).messageId()
            logger.debug { "email sent with messageId[$messageId]" }
        } catch (e: SesException) {
            logger.error("failed to send email to [$emailToRecipients], cc[$emailCcRecipients] with subject[$subject]", e)
            throw EmailNotSentException(emailToRecipients, emailCcRecipients, subject, e)
        }
        return emailToRecipients + emailCcRecipients
    }
}
