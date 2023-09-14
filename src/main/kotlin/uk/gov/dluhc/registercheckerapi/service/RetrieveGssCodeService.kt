package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException

private val logger = KotlinLogging.logger {}

@Service
class RetrieveGssCodeService(
    private val ierApiClient: IerApiClient,
) {
    fun getGssCodeFromCertificateSerial(certificateSerial: String): List<String> {
        val eros = ierApiClient.getEros()
        val ero = eros.find { it.activeClientCertificateSerials.contains(certificateSerial) }
        if (ero == null) {
            throw IerEroNotFoundException(certificateSerial).apply {
                logger.warn { "Certificate serial not found" }
            }
        }
        return ero.localAuthorities.map { it.gssCode }
    }
}
