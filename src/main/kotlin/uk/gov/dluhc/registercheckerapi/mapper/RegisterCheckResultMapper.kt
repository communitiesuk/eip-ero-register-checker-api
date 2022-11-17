package uk.gov.dluhc.registercheckerapi.mapper

import org.apache.commons.lang3.StringUtils
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
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
    @Autowired
    protected lateinit var instantMapper: InstantMapper

    @Mapping(target = "requestId", source = "queryParamRequestId")
    @Mapping(target = "correlationId", source = "apiRequest.requestid")
    @Mapping(target = "matchResultSentAt", source = "apiRequest.createdAt")
    @Mapping(target = "matchCount", source = "apiRequest.registerCheckMatchCount")
    abstract fun fromRegisterCheckResultRequestApiToDto(queryParamRequestId: UUID, apiRequest: RegisterCheckResultRequest): RegisterCheckResultDto

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

    @Named("trimAndToUppercase")
    fun trimAndToUppercase(source: String?): String? = StringUtils.toRootUpperCase(StringUtils.trim(source))
}
