package uk.gov.dluhc.registercheckerapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckRemovalDto
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper

private val logger = KotlinLogging.logger {}

@Service
class RegisterCheckRemovalService(
    private val registerCheckRepository: RegisterCheckRepository,
    private val sourceTypeMapper: SourceTypeMapper,
) {

    fun removeRegisterCheckData(dto: RegisterCheckRemovalDto) {
        with(dto) {
            logger.info("Finding records to delete for sourceType: [$sourceType], sourceReference: [$sourceReference]")
            val matchingRecords = registerCheckRepository.findBySourceTypeAndSourceReference(
                sourceType = sourceTypeMapper.fromDtoToEntityEnum(sourceType),
                sourceReference = sourceReference
            )
            if (CollectionUtils.isEmpty(matchingRecords)) {
                logger.info("No matching record found to delete for sourceType: [$sourceType], sourceReference: [$sourceReference]")
            } else {
                logger.info("Deleting [${matchingRecords.size}] record(s) for sourceType: [$sourceType], sourceReference: [$sourceReference]")
                registerCheckRepository.deleteAll(matchingRecords)
            }
        }
        // TODO next subtask to remove from register check result data entity as well
    }
}
