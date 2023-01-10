package uk.gov.dluhc.registercheckerapi.messaging.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckRemovalDto
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage

@Mapper(uses = [SourceTypeMapper::class])
interface RegisterCheckRemovalMapper {

    fun toRemovalDto(message: RemoveRegisterCheckDataMessage): RegisterCheckRemovalDto
}
