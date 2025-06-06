package uk.gov.dluhc.registercheckerapi.config

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties

private val logger = KotlinLogging.logger { }

@ConfigurationProperties(prefix = "feature-toggles")
data class FeatureToggleConfiguration(
    val enableRegisterCheckToEmsMessageForwarding: Boolean = false,
) {
    @PostConstruct
    fun logConfiguration() {
        logger.info { "feature-toggles: $this" }
    }
}
