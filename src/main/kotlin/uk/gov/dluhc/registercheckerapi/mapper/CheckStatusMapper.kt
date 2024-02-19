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

    @ValueMapping(target = "EXACT_MINUS_MATCH", source = "EXACT_MATCH")
    @ValueMapping(target = "PARTIAL_MINUS_MATCH", source = "PARTIAL_MATCH")
    @ValueMapping(target = "NO_MINUS_MATCH", source = "NO_MATCH")
    @ValueMapping(target = "MULTIPLE_MINUS_MATCH", source = "MULTIPLE_MATCH")
    @ValueMapping(target = "TOO_MINUS_MANY_MINUS_MATCHES", source = "TOO_MANY_MATCHES")
    @ValueMapping(target = "PENDING_MINUS_DETERMINATION", source = "PENDING_DETERMINATION")
    @ValueMapping(target = "EXPIRED", source = "EXPIRED")
    @ValueMapping(target = "NOT_MINUS_STARTED", source = "NOT_STARTED")
    @ValueMapping(target = MappingConstants.THROW_EXCEPTION, source = "PENDING")
    @ValueMapping(target = MappingConstants.THROW_EXCEPTION, source = "ARCHIVED")
    fun toRegisterCheckResultEnum(checkStatus: CheckStatus): RegisterCheckResult
}
