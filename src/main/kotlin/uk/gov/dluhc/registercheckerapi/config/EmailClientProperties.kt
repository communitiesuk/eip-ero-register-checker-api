package uk.gov.dluhc.registercheckerapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Class to bind configuration properties for the email client
 */
@ConfigurationProperties(prefix = "email.client")
data class EmailClientProperties(
    val sender: String,
)
