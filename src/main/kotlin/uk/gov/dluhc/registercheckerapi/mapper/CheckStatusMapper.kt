package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ValueMapping
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult

@Mapper
interface CheckStatusMapper {

    fun toCheckStatusEntityEnum(registerCheckStatus: RegisterCheckStatus): CheckStatus

    @ValueMapping(target = MappingConstants.THROW_EXCEPTION, source = "PENDING")
    fun toRegisterCheckStatusResultEnum(checkStatus: CheckStatus): RegisterCheckResult
}
