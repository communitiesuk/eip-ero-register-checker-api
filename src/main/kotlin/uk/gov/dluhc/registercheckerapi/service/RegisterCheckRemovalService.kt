package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.messaging.dto.RegisterCheckRemovalDto
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckRemovalService(
    private val registerCheckRepository: RegisterCheckRepository,
    private val registerCheckResultDataRepository: RegisterCheckResultDataRepository,
    private val sourceTypeMapper: SourceTypeMapper,
) {

    fun removeRegisterCheckData(dto: RegisterCheckRemovalDto) {
        val correlationIds = removeRegisterCheck(dto)
        removeRegisterCheckResult(correlationIds)
    }

    private fun removeRegisterCheck(dto: RegisterCheckRemovalDto): Set<UUID> {
        with(dto) {
            logger.info("Finding RegisterCheck for removal for sourceType: [$sourceType], sourceReference: [$sourceReference]")
            val matchingRecords = registerCheckRepository.findBySourceReferenceAndSourceType(
                sourceReference = sourceReference,
                sourceType = sourceTypeMapper.fromDtoToEntityEnum(sourceType),
            )
            if (CollectionUtils.isEmpty(matchingRecords)) {
                logger.info("Found no matching RegisterCheck to delete for sourceType: [$sourceType], sourceReference: [$sourceReference]")
            } else {
                logger.info("Deleting [${matchingRecords.size}] RegisterCheck record(s) for sourceType: [$sourceType], sourceReference: [$sourceReference]")
                registerCheckRepository.deleteAll(matchingRecords)
            }
            return matchingRecords.map { it.correlationId }.toSet()
        }
    }

    private fun removeRegisterCheckResult(correlationIds: Set<UUID>) {
        if (correlationIds.isNotEmpty()) {
            logger.info("Finding RegisterCheckResult records for removal for [${correlationIds.size}] correlationIds: $correlationIds")
            val matchingRecords = registerCheckResultDataRepository.findByCorrelationIdIn(correlationIds)

            if (CollectionUtils.isEmpty(matchingRecords)) {
                logger.info("Found no matching RegisterCheckResult to delete for [${correlationIds.size}] correlationIds: $correlationIds")
            } else {
                logger.info("Deleting [${matchingRecords.size}] RegisterCheckResult record(s) for [${correlationIds.size}] correlationIds: $correlationIds")
                registerCheckResultDataRepository.deleteAll(matchingRecords)
            }
        }
    }
}
