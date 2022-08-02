package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient

private val logger = KotlinLogging.logger {}

@Service
class IerService(private val ierApiClient: IerApiClient) {

    @Cacheable("certificateSerialToEroIdCache")
    fun getEroIdentifierForCertificateSerial(certificateSerial: String): String =
        ierApiClient.getEroIdentifier(certificateSerial).eroId!!
}
