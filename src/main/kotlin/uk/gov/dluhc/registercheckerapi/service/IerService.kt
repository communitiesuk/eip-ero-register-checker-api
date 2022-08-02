package uk.gov.dluhc.registercheckerapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.config.CachingConfig

@Service
class IerService(private val ierApiClient: IerApiClient) {

    @Cacheable(CachingConfig.CERTIFICATE_SERIAL_CACHE_NAME)
    fun getEroIdentifierForCertificateSerial(certificateSerial: String): String =
        ierApiClient.getEroIdentifier(certificateSerial).eroId!!
}
