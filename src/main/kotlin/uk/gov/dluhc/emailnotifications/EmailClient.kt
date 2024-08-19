package uk.gov.dluhc.emailnotifications

/**
 * A generic interface to allow access to an email server
 */
interface EmailClient {
    /**
     * @param emailToRecipients recipients that the email will be sent to
     * @param emailCcRecipients recipients that the email will be CC'd to
     * @param subject the subject of the email
     * @param emailHtmlBody the HTML body of the email
     * @return a consolidated list of the email addresses that this email was sent to including those CC'd.
     * If the email client implementation filters the recipient lists then the set of recipients returned could be empty
     * (e.g. if it filtered based on allow listed domains, then only those allow listed recipients will be returned).
     * @throws EmailNotSentException if an error occurs while sending the email
     */
    fun send(
        emailToRecipients: Set<String>,
        emailCcRecipients: Set<String> = emptySet(),
        subject: String,
        emailHtmlBody: String,
    ): Set<String>
}
