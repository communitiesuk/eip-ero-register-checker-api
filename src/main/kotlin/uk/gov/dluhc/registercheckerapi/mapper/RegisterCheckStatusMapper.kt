package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus

@Mapper
interface RegisterCheckStatusMapper {

    fun toCheckStatusEntityEnum(registerCheckStatus: RegisterCheckStatus): CheckStatus
}