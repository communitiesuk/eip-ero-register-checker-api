package uk.gov.dluhc.registercheckerapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.ses.SesClient
import uk.gov.dluhc.email.SesEmailClient

@Configuration
class EmailConfiguration {
    @Bean
    fun sesEmailClient(
        sesClient: SesClient,
        emailClientProperties: EmailClientProperties
    ): SesEmailClient =
        with(emailClientProperties) {
            SesEmailClient(
                sesClient = sesClient,
                sender = sender,
                allowListEnabled = false,
                allowListDomains = emptySet()
            )
        }
}
