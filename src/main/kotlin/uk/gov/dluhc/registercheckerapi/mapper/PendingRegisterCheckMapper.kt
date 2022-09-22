package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ValueMapping
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.dto.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterCheck
import uk.gov.dluhc.registercheckerapi.models.SourceSystem

/**
 * Maps incoming [InitiateRegisterCheckMessage] to [PendingRegisterCheckDto]. Maps the entity class [RegisterCheck]
 * to/from the corresponding [PendingRegisterCheckDto].
 */
@Mapper
interface PendingRegisterCheckMapper {

    @Mapping(target = "correlationId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdBy", source = "requestedBy")
    fun initiateRegisterCheckMessageToPendingRegisterCheckDto(initiateRegisterCheckMessage: InitiateRegisterCheckMessage): PendingRegisterCheckDto

    @Mapping(target = "status", constant = "PENDING")
    fun pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto: PendingRegisterCheckDto): RegisterCheck

    @Mapping(target = "createdAt", source = "dateCreated")
    fun registerCheckEntityToPendingRegisterCheckDto(registerCheck: RegisterCheck): PendingRegisterCheckDto

    @Mapping(target = "requestid", source = "correlationId")
    @Mapping(target = "source", source = "sourceType")
    @Mapping(target = "gssCode", source = "gssCode")
    @Mapping(target = "actingStaffId", constant = "EROP") // TODO - will contain userId when manual checks are allowed in future
    @Mapping(target = "fn", source = "pendingRegisterCheckDto.personalDetail.firstName")
    @Mapping(target = "mn", source = "pendingRegisterCheckDto.personalDetail.middleNames")
    @Mapping(target = "ln", source = "pendingRegisterCheckDto.personalDetail.surname")
    @Mapping(target = "dob", source = "pendingRegisterCheckDto.personalDetail.dateOfBirth")
    @Mapping(target = "phone", source = "pendingRegisterCheckDto.personalDetail.phone")
    @Mapping(target = "email", source = "pendingRegisterCheckDto.personalDetail.email")
    @Mapping(target = "regproperty", source = "pendingRegisterCheckDto.personalDetail.address.property")
    @Mapping(target = "regstreet", source = "pendingRegisterCheckDto.personalDetail.address.street")
    @Mapping(target = "regpostcode", source = "pendingRegisterCheckDto.personalDetail.address.postcode")
    @Mapping(target = "reglocality", source = "pendingRegisterCheckDto.personalDetail.address.locality")
    @Mapping(target = "regtown", source = "pendingRegisterCheckDto.personalDetail.address.town")
    @Mapping(target = "regarea", source = "pendingRegisterCheckDto.personalDetail.address.area")
    @Mapping(target = "reguprn", source = "pendingRegisterCheckDto.personalDetail.address.uprn")
    @Mapping(target = "createdAt", expression = "java(pendingRegisterCheckDto.getCreatedAt().atOffset(java.time.ZoneOffset.UTC))")
    fun pendingRegisterCheckDtoToPendingRegisterCheckModel(pendingRegisterCheckDto: PendingRegisterCheckDto): PendingRegisterCheck

    @Mapping(target = "phoneNumber", source = "phone")
    fun personalDetailDtoToPersonalDetailEntity(personalDetailDto: PersonalDetailDto): PersonalDetail

    @Mapping(target = "phone", source = "phoneNumber")
    fun personalDetailEntityToPersonalDetailDto(personalDetail: PersonalDetail): PersonalDetailDto

    @ValueMapping(source = "VOTER_CARD", target = "EROP")
    fun sourceTypeToSourceSystem(sourceType: SourceType): SourceSystem
}
