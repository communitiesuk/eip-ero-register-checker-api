package uk.gov.dluhc.registercheckerapi.mapper

import org.apache.commons.lang3.StringUtils
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.dto.VotingArrangementDto
import uk.gov.dluhc.registercheckerapi.models.PostalVote
import uk.gov.dluhc.registercheckerapi.models.ProxyVote
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import java.util.UUID
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch as RegisterCheckMatchEntity

/**
 * Maps [RegisterCheckResultRequest] to [RegisterCheckResultDto].
 */
@Mapper(
    uses = [
        InstantMapper::class,
        PersonalDetailMapper::class
    ]
)
abstract class RegisterCheckResultMapper {
    @Mapping(target = "requestId", source = "queryParamRequestId")
    @Mapping(target = "correlationId", source = "apiRequest.requestid")
    @Mapping(target = "matchResultSentAt", source = "apiRequest.createdAt")
    @Mapping(target = "matchCount", source = "apiRequest.registerCheckMatchCount")
    abstract fun fromRegisterCheckResultRequestApiToDto(queryParamRequestId: UUID, apiRequest: RegisterCheckResultRequest): RegisterCheckResultDto

    @Mapping(target = "postalVotingArrangement", source = "postalVote")
    @Mapping(target = "proxyVotingArrangement", source = "proxyVote")
    abstract fun fromDtoToRegisterCheckMatchEntity(registerCheckMatchDto: RegisterCheckMatchDto): RegisterCheckMatchEntity

    @Mapping(target = "personalDetail", source = ".")
    @Mapping(target = "franchiseCode", source = "franchiseCode", qualifiedByName = ["trimAndToUppercase"])
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

    @Mapping(target = "untilFurtherNotice", source = "postalVoteUntilFurtherNotice")
    @Mapping(target = "forSingleDate", source = "postalVoteForSingleDate")
    @Mapping(target = "startDate", source = "postalVoteStartDate")
    @Mapping(target = "endDate", source = "postalVoteEndDate")
    protected abstract fun fromPostalVoteToVotingArrangementDto(postalVoteApi: PostalVote): VotingArrangementDto

    @Mapping(target = "untilFurtherNotice", source = "proxyVoteUntilFurtherNotice")
    @Mapping(target = "forSingleDate", source = "proxyVoteForSingleDate")
    @Mapping(target = "startDate", source = "proxyVoteStartDate")
    @Mapping(target = "endDate", source = "proxyVoteEndDate")
    protected abstract fun fromProxyVoteToVotingArrangementDto(proxyVoteApi: ProxyVote): VotingArrangementDto

    @Named("trimAndToUppercase")
    fun trimAndToUppercase(source: String?): String? = StringUtils.toRootUpperCase(StringUtils.trim(source))
}
