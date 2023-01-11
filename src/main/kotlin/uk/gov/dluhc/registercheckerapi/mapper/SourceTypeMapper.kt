package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.registercheckerapi.dto.SourceType
import uk.gov.dluhc.registercheckerapi.models.SourceSystem
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as SourceTypeEntityEnum
import uk.gov.dluhc.registercheckerapi.dto.SourceType as SourceTypeDtoEnum
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType as SourceTypeSqsEnum

@Mapper
interface SourceTypeMapper {

    @ValueMapping(target = "VOTER_CARD", source = "VOTER_MINUS_CARD")
    fun fromSqsToDtoEnum(sqsSourceType: SourceTypeSqsEnum): SourceTypeDtoEnum

    @ValueMapping(target = "VOTER_MINUS_CARD", source = "VOTER_CARD")
    fun fromEntityToSqsEnum(entitySourceType: SourceTypeEntityEnum): SourceTypeSqsEnum

    fun fromEntityToDtoEnum(entitySourceType: SourceTypeEntityEnum): SourceTypeDtoEnum

    @ValueMapping(target = "EROP", source = "VOTER_CARD")
    fun sourceTypeDtoToSourceSystem(sourceType: SourceType): SourceSystem
}
