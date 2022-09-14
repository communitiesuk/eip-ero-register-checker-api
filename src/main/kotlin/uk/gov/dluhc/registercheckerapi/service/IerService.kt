package uk.gov.dluhc.registercheckerapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerGetEroApiClient

@Service
class IerService(private val ierGetEroApiClient: IerGetEroApiClient) {

    fun getEroIdentifierForCertificateSerial(certificateSerial: String): String =
        ierGetEroApiClient.getEroIdentifier(certificateSerial).eroId!!
}
