package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ValueMapping
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult

@Mapper
interface RegisterCheckStatusMapper {

    fun toCheckStatusEntityEnum(registerCheckStatus: RegisterCheckStatus): CheckStatus

    @ValueMapping(target = "NO_MATCH", source = "NO_MATCH")
    @ValueMapping(target = "EXACT_MATCH", source = "EXACT_MATCH")
    @ValueMapping(target = "MULTIPLE_MATCH", source = "MULTIPLE_MATCH")
    @ValueMapping(target = "TOO_MANY_MATCHES", source = "TOO_MANY_MATCHES")
    @ValueMapping(target = MappingConstants.THROW_EXCEPTION, source = MappingConstants.ANY_UNMAPPED)
    fun fromCheckStatusEntityToRegisterCheckStatusResultEnum(checkStatus: CheckStatus): RegisterCheckResult
}
