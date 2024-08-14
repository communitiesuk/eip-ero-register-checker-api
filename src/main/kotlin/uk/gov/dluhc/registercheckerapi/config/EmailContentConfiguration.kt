package uk.gov.dluhc.registercheckerapi.config

import com.amazonaws.util.IOUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.ClassPathResource

/**
 * Class to bind configuration properties for the email content
 */
@ConfigurationProperties(prefix = "email.pending-register-checks-content")
@ConstructorBinding
class EmailContentConfiguration(
    val subject: String,
    emailBodyTemplate: String,
    val recipients: String
) {
    val emailBody: String

    init {
        with(ClassPathResource(emailBodyTemplate)) {
            emailBody = inputStream.use {
                IOUtils.toString(it)
            }
        }
    }
}
