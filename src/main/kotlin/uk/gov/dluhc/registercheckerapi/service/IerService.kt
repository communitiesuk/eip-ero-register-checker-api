package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerManagementApiClient
import uk.gov.dluhc.registercheckerapi.client.IerManagementApiException

private val logger = KotlinLogging.logger {}

@Service
class IerService(private val ierManagementApiClient: IerManagementApiClient) {

    fun getEroIdentifierForCertificateSerial(certificateSerial: String): String =
        try {
            ierManagementApiClient.getEroIdentifier(certificateSerial).let { it.eroId!! }
        } catch (ex: IerManagementApiException) {
            logger.info { "Unable to retrieve EROCertificateMapping for certificate serial [$certificateSerial] due to error: [${ex.message}]" }
            throw ex
        }
}
