package uk.gov.dluhc.registercheckerapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient

@Service
class RetrieveGssCodeService(
    private val ierApiClient: IerApiClient,
    private val eroService: EroService,
) {
    fun getGssCodeFromCertificateSerial(certificateSerial: String): List<String> {
        val eroIdFromIer = ierApiClient.getEroIdentifier(certificateSerial).eroId!!
        return eroService.lookupGssCodesForEro(eroIdFromIer)
    }
}
