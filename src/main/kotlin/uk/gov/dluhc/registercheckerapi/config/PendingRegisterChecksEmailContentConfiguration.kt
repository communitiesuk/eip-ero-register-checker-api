package uk.gov.dluhc.registercheckerapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ClassPathResource
import software.amazon.awssdk.utils.IoUtils

/**
 * Class to bind configuration properties for the email content
 */
@ConfigurationProperties(prefix = "email.pending-register-checks-content")
class PendingRegisterChecksEmailContentConfiguration(
    val subject: String,
    emailBodyTemplate: String,
    val recipients: String,
) {
    val emailBody: String

    init {
        with(ClassPathResource(emailBodyTemplate)) {
            emailBody = inputStream.use(IoUtils::toUtf8String)
        }
    }
}
