package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerApiException

private val logger = KotlinLogging.logger {}

@Service
class IerService(private val ierApiClient: IerApiClient) {

    fun getEroIdentifierForCertificateSerial(certificateSerial: String): String =
        try {
            ierApiClient.getEroIdentifier(certificateSerial).let { it.eroId!! }
        } catch (ex: IerApiException) {
            logger.info { "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [${ex.message}]" }
            throw ex
        }
}
