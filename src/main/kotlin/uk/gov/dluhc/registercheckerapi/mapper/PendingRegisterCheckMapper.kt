package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
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
@Mapper(uses = [InstantMapper::class])
abstract class PendingRegisterCheckMapper {

    @Mapping(target = "correlationId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdBy", source = "requestedBy")
    abstract fun initiateRegisterCheckMessageToPendingRegisterCheckDto(initiateRegisterCheckMessage: InitiateRegisterCheckMessage): PendingRegisterCheckDto

    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "registerCheckMatches", expression = "java(new java.util.ArrayList())")
    abstract fun pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto: PendingRegisterCheckDto): RegisterCheck

    @Mapping(target = "createdAt", source = "dateCreated")
    abstract fun registerCheckEntityToPendingRegisterCheckDto(registerCheck: RegisterCheck): PendingRegisterCheckDto

    @Mapping(target = "requestid", source = "correlationId")
    @Mapping(target = "source", source = "sourceType")
    @Mapping(target = "gssCode", source = "gssCode")
    @Mapping(target = "actingStaffId", source = "createdBy", qualifiedByName = ["createdByToActingStaffId"])
    @Mapping(target = "fn", source = "personalDetail.firstName")
    @Mapping(target = "mn", source = "personalDetail.middleNames")
    @Mapping(target = "ln", source = "personalDetail.surname")
    @Mapping(target = "dob", source = "personalDetail.dateOfBirth")
    @Mapping(target = "phone", source = "personalDetail.phone")
    @Mapping(target = "email", source = "personalDetail.email")
    @Mapping(target = "regproperty", source = "personalDetail.address.property")
    @Mapping(target = "regstreet", source = "personalDetail.address.street")
    @Mapping(target = "regpostcode", source = "personalDetail.address.postcode")
    @Mapping(target = "reglocality", source = "personalDetail.address.locality")
    @Mapping(target = "regtown", source = "personalDetail.address.town")
    @Mapping(target = "regarea", source = "personalDetail.address.area")
    @Mapping(target = "reguprn", source = "personalDetail.address.uprn")
    abstract fun pendingRegisterCheckDtoToPendingRegisterCheckModel(pendingRegisterCheckDto: PendingRegisterCheckDto): PendingRegisterCheck

    @Mapping(target = "phoneNumber", source = "phone")
    protected abstract fun personalDetailDtoToPersonalDetailEntity(personalDetailDto: PersonalDetailDto): PersonalDetail

    @InheritInverseConfiguration
    protected abstract fun personalDetailEntityToPersonalDetailDto(personalDetail: PersonalDetail): PersonalDetailDto

    @ValueMapping(source = "VOTER_CARD", target = "EROP")
    protected abstract fun sourceTypeToSourceSystem(sourceType: SourceType): SourceSystem

    @Named("createdByToActingStaffId")
    protected fun createdByToActingStaffId(createdBy: String): String {
        return when (createdBy) {
            "system" -> "EROP"
            else -> createdBy
        }
    }
}
