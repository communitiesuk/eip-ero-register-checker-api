package uk.gov.dluhc.registercheckerapi.validator

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.exception.RegisterCheckMatchCountMismatchException
import uk.gov.dluhc.registercheckerapi.exception.RequestIdMismatchException

private val logger = KotlinLogging.logger {}

@Component
class RegisterCheckRequestValidator {

    fun validateRequestBody(certificateSerial: String, registerCheckResultDto: RegisterCheckResultDto) {
        validateRequestIdMatch(registerCheckResultDto)
        validateMatchCountWithRegisterCheckMatchList(registerCheckResultDto)
    }

    private fun validateRequestIdMatch(registerCheckResultDto: RegisterCheckResultDto) {
        if (registerCheckResultDto.requestId != registerCheckResultDto.correlationId) {
            throw RequestIdMismatchException(registerCheckResultDto.requestId, registerCheckResultDto.correlationId)
                .also { logger.warn { it.message } }
        }
    }

    private fun validateMatchCountWithRegisterCheckMatchList(registerCheckResultDto: RegisterCheckResultDto) {
        when (registerCheckResultDto.matchCount) {
            in 1..10 -> {
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
}
