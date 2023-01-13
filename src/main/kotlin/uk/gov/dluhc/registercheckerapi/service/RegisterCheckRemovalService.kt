package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckRemovalDto
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckRemovalService(
    private val registerCheckRepository: RegisterCheckRepository,
    private val registerCheckResultDataRepository: RegisterCheckResultDataRepository,
    private val sourceTypeMapper: SourceTypeMapper,
) {

    fun removeRegisterCheckData(dto: RegisterCheckRemovalDto) {
        removeRegisterCheck(dto).also { removeRegisterCheckResult(it) }
    }

    private fun removeRegisterCheck(dto: RegisterCheckRemovalDto): List<UUID> {
        with(dto) {
            logger.info("Finding RegisterCheck to delete for sourceType: [$sourceType], sourceReference: [$sourceReference]")
            val matchingRecords = registerCheckRepository.findBySourceTypeAndSourceReference(
                sourceType = sourceTypeMapper.fromDtoToEntityEnum(sourceType),
                sourceReference = sourceReference
            )
            if (CollectionUtils.isEmpty(matchingRecords)) {
                logger.info("Found no matching RegisterCheck to delete for sourceType: [$sourceType], sourceReference: [$sourceReference]")
            } else {
                logger.info("Deleting [${matchingRecords.size}] RegisterCheck record(s) for sourceType: [$sourceType], sourceReference: [$sourceReference]")
                registerCheckRepository.deleteAll(matchingRecords)
            }
            return matchingRecords.map { it.correlationId }.toList()
        }
    }

    private fun removeRegisterCheckResult(correlationIds: List<UUID>) {
        if (correlationIds.isNotEmpty()) {
            logger.info("Finding RegisterCheckResult records to delete for correlationIds: $correlationIds")
            val matchingRecords = registerCheckResultDataRepository.findByCorrelationIdIn(correlationIds.distinct())

            if (CollectionUtils.isEmpty(matchingRecords)) {
                logger.info("Found no matching RegisterCheckResult to delete for correlationIds: $correlationIds")
            } else {
                logger.info("Deleting [${matchingRecords.size}] RegisterCheckResult record(s) for correlationIds: $correlationIds")
                registerCheckResultDataRepository.deleteAll(matchingRecords)
            }
        }
    }
}
