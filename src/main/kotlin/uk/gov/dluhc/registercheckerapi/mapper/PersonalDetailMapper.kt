package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto

@Mapper
interface PersonalDetailMapper {

    @Mapping(target = "phoneNumber", source = "phone")
    fun personalDetailDtoToPersonalDetailEntity(personalDetailDto: PersonalDetailDto): PersonalDetail

    @InheritInverseConfiguration
    fun personalDetailEntityToPersonalDetailDto(personalDetail: PersonalDetail): PersonalDetailDto
}
