package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.registercheckerapi.dto.SourceType
import uk.gov.dluhc.registercheckerapi.models.SourceSystem
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as SourceTypeEntityEnum
import uk.gov.dluhc.registercheckerapi.dto.SourceType as SourceTypeDtoEnum
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType as SourceTypeRcaSqsEnum
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.SourceType as SourceTypeVcaSqsEnum

@Mapper
interface SourceTypeMapper {

    @ValueMapping(target = "VOTER_CARD", source = "VOTER_MINUS_CARD")
    fun fromSqsToDtoEnum(sqsSourceType: SourceTypeRcaSqsEnum): SourceTypeDtoEnum

    @ValueMapping(target = "VOTER_MINUS_CARD", source = "VOTER_CARD")
    fun fromEntityToVcaSqsEnum(entitySourceType: SourceTypeEntityEnum): SourceTypeVcaSqsEnum

    fun fromEntityToDtoEnum(entitySourceType: SourceTypeEntityEnum): SourceTypeDtoEnum

    @InheritInverseConfiguration
    fun fromDtoToEntityEnum(dtoSourceType: SourceTypeDtoEnum): SourceTypeEntityEnum

    @ValueMapping(target = "EROP", source = "VOTER_CARD")
    fun sourceTypeDtoToSourceSystem(sourceType: SourceType): SourceSystem
}
