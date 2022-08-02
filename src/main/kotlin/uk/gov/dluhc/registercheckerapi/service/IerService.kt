package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.config.CachingConfig

private val logger = KotlinLogging.logger {}

@Service
class IerService(private val ierApiClient: IerApiClient) {

    @Cacheable(CachingConfig.CERTIFICATE_SERIAL_CACHE_NAME)
    fun getEroIdentifierForCertificateSerial(certificateSerial: String): String =
        ierApiClient.getEroIdentifier(certificateSerial).eroId!!
}
