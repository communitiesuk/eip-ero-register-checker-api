package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage

/**
 * Maps [RegisterCheck] to [RegisterCheckResultMessage].
 */
@Mapper(
    uses = [
        CheckStatusMapper::class
    ]
)
abstract class RegisterCheckResultMessageMapper {

    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "sourceReference", source = "sourceReference")
    @Mapping(target = "sourceCorrelationId", source = "sourceCorrelationId")
    @Mapping(target = "registerCheckResult", source = "status")
    @Mapping(target = "matches", source = "registerCheckMatches")
    abstract fun fromRegisterCheckEntityToRegisterCheckResultMessage(entity: RegisterCheck): RegisterCheckResultMessage

    @Mapping(target = "firstName", source = "personalDetail.firstName")
    @Mapping(target = "middleNames", source = "personalDetail.middleNames")
    @Mapping(target = "surname", source = "personalDetail.surname")
    @Mapping(target = "dateOfBirth", source = "personalDetail.dateOfBirth")
    @Mapping(target = "email", source = "personalDetail.email")
    @Mapping(target = "phone", source = "personalDetail.phoneNumber")
    @Mapping(target = "address", source = "personalDetail.address")
    protected abstract fun registerCheckMatchToRegisterCheckPersonalDetail(registerCheckMatchEntity: RegisterCheckMatch): RegisterCheckPersonalDetail
}
