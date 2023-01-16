package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.RegisterCheckMatch as RegisterCheckMatchSqsModel

/**
 * Maps [RegisterCheck] to [RegisterCheckResultMessage].
 */
@Mapper(
    uses = [
        CheckStatusMapper::class,
        SourceTypeMapper::class,
    ]
)
abstract class RegisterCheckResultMessageMapper {

    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "sourceReference", source = "sourceReference")
    @Mapping(target = "sourceCorrelationId", source = "sourceCorrelationId")
    @Mapping(target = "registerCheckResult", source = "status")
    @Mapping(target = "matches", source = "registerCheckMatches")
    abstract fun fromRegisterCheckEntityToRegisterCheckResultMessage(entity: RegisterCheck): RegisterCheckResultMessage

    protected abstract fun registerCheckMatchEntityToRegisterCheckPersonalDetailModel(registerCheckMatchEntity: RegisterCheckMatch): RegisterCheckMatchSqsModel

    @Mapping(target = "firstName", source = "personalDetail.firstName")
    @Mapping(target = "middleNames", source = "personalDetail.middleNames")
    @Mapping(target = "surname", source = "personalDetail.surname")
    @Mapping(target = "dateOfBirth", source = "personalDetail.dateOfBirth")
    @Mapping(target = "email", source = "personalDetail.email")
    @Mapping(target = "phone", source = "personalDetail.phoneNumber")
    @Mapping(target = "address", source = "personalDetail.address")
    protected abstract fun personalDetailEntityToRegisterCheckPersonalDetailModel(personalDetail: PersonalDetail): RegisterCheckPersonalDetail
}
