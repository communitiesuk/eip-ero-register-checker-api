package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.EroIdNotFoundException
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.client.IerEroNotFoundException

private val logger = KotlinLogging.logger {}

@Service
class RetrieveGssCodeService(
    private val ierApiClient: IerApiClient,
) {
    fun getGssCodesFromCertificateSerial(certificateSerial: String): List<String> {
        val eros = ierApiClient.getEros()
        val ero = eros.find { it.activeClientCertificateSerials.contains(certificateSerial) }
        if (ero == null) {
            throw IerEroNotFoundException(certificateSerial)
                .also { logger.warn { "Certificate serial not found" } }
        }
        return ero.localAuthorities.map { it.gssCode }
    }

    fun getGssCodesFromEroId(eroId: String): List<String> {
        val eros = ierApiClient.getEros()
        val ero = eros.find { it.eroIdentifier == eroId }
        if (ero == null) {
            throw EroIdNotFoundException(eroId).also { logger.warn { "ERO ID not found" } }
        }
        return ero.localAuthorities.map { it.gssCode }
    }
}
