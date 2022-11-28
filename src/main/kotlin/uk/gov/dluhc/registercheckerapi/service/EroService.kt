package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeManagementApiException

private val logger = KotlinLogging.logger {}

/**
 * Service class with Electoral Registration Office (ERO) functionality.
 */
@Service
class EroService(private val electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient) {

    fun lookupGssCodesForEro(eroId: String): List<String> =
        try {
            electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(eroId).localAuthorities
                .map { it.gssCode }
                .apply {
                    logger.debug { "GET [ero-management] response for eroId=[$eroId] is $this" }
                }
        } catch (ex: ElectoralRegistrationOfficeManagementApiException) {
            logger.info { "Error ${ex.message} returned whilst looking up the gssCodes for ERO $eroId" }
            throw ex
        }
}
