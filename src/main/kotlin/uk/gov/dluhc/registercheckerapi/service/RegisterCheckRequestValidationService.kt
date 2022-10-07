package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.registercheckerapi.client.IerApiClient
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.exception.GssCodeMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckMatchCountMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckRequestValidationService(
    private val ierApiClient: IerApiClient,
    private val eroService: EroService
) {

    fun validateRequestBody(certificateSerial: String, registerCheckResultDto: RegisterCheckResultDto) {
        validateRequestIdMatch(registerCheckResultDto)
        validateMatchCountWithRegisterCheckMatchList(registerCheckResultDto)
        validateGssCodeMatch(certificateSerial, registerCheckResultDto.gssCode)
    }

    private fun validateRequestIdMatch(registerCheckResultDto: RegisterCheckResultDto) {
        if (registerCheckResultDto.requestId != registerCheckResultDto.correlationId) {
            throw RequestIdMismatchException(registerCheckResultDto.requestId, registerCheckResultDto.correlationId)
                .also { logger.warn { it.message } }
        }
    }

    private fun validateMatchCountWithRegisterCheckMatchList(registerCheckResultDto: RegisterCheckResultDto) {
        when (registerCheckResultDto.matchCount) {
            in 0..10 -> {
                val matchResultsSize = registerCheckResultDto.registerCheckMatches?.size
                if (registerCheckResultDto.matchCount != matchResultsSize) {
                    val errorMessage = "Request [registerCheckMatches:$matchResultsSize] array size must be same as " +
                        "[registerCheckMatchCount:${registerCheckResultDto.matchCount}] in body payload"
                    throw RegisterCheckMatchCountMismatchException(errorMessage).also { logger.warn { it.message } }
                }
            }

            else -> {
                if (registerCheckResultDto.registerCheckMatches?.isEmpty() == false) {
                    val errorMessage = "Request [registerCheckMatches] array must be null or empty for " +
                        "[registerCheckMatchCount:${registerCheckResultDto.matchCount}] in body payload"
                    throw RegisterCheckMatchCountMismatchException(errorMessage).also { logger.warn { it.message } }
                }
            }
        }
    }

    private fun validateGssCodeMatch(certificateSerial: String, requestGssCode: String) {
        val eroIdFromIer = ierApiClient.getEroIdentifier(certificateSerial).eroId!!
        if (requestGssCode !in eroService.lookupGssCodesForEro(eroIdFromIer))
            throw GssCodeMismatchException(certificateSerial, requestGssCode)
                .also { logger.warn { it.message } }
    }
}
