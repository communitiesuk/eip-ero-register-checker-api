package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest

/**
 * Maps [RegisterCheckResultRequest] to [RegisterCheckResultDto].
 */
@Mapper(uses = [InstantMapper::class])
abstract class RegisterCheckResultMapper {

    @Mapping(target = "requestId", expression = "java(UUID.fromString(queryParamRequestId))")
    @Mapping(target = "correlationId", source = "apiRequest.requestid")
    @Mapping(target = "matchResultSentAt", source = "apiRequest.createdAt")
    @Mapping(target = "matchCount", source = "apiRequest.registerCheckMatchCount")
    @Mapping(target = "registerCheckStatus", source = "apiRequest.registerCheckMatchCount", qualifiedByName = ["evaluateRegisterCheckStatus"])
    @Mapping(target = "registerCheckMatchDto", source = "apiRequest.registerCheckMatches")
    abstract fun fromRegisterCheckResultRequestApiToDto(queryParamRequestId: String, apiRequest: RegisterCheckResultRequest): RegisterCheckResultDto

    @Mapping(target = "personalDetail", source = ".")
    protected abstract fun fromRegisterCheckMatchApiToDto(registerCheckMatchApi: RegisterCheckMatch): RegisterCheckMatchDto

    @Mapping(target = "firstName", source = "fn")
    @Mapping(target = "middleNames", source = "mn")
    @Mapping(target = "surname", source = "ln")
    @Mapping(target = "dateOfBirth", source = "dob")
    @Mapping(target = "address", source = ".")
    protected abstract fun toPersonalDetailDto(registerCheckMatchApi: RegisterCheckMatch): PersonalDetailDto

    @Mapping(target = "property", source = "regproperty")
    @Mapping(target = "street", source = "regstreet")
    @Mapping(target = "postcode", source = "regpostcode")
    @Mapping(target = "locality", source = "reglocality")
    @Mapping(target = "town", source = "regtown")
    @Mapping(target = "area", source = "regarea")
    @Mapping(target = "uprn", source = "reguprn")
    protected abstract fun toAddressDto(registerCheckMatchApi: RegisterCheckMatch): AddressDto

    @Named("evaluateRegisterCheckStatus")
    protected fun evaluateRegisterCheckStatus(registerCheckMatchCount: Int): RegisterCheckStatus =
        when (registerCheckMatchCount) {
            0 -> RegisterCheckStatus.NO_MATCH
            1 -> RegisterCheckStatus.EXACT_MATCH
            in 2..10 -> RegisterCheckStatus.MULTIPLE_MATCH
            else -> RegisterCheckStatus.TOO_MANY_MATCHES
        }
}
