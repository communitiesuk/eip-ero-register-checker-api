package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterCheck

/**
 * Maps the entity class [RegisterCheck] to/from the corresponding [PendingRegisterCheckDto].
 */
@Mapper(
    uses = [
        InstantMapper::class,
        PersonalDetailMapper::class,
        SourceTypeMapper::class,
    ]
)
abstract class PendingRegisterCheckMapper {

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

    @Named("createdByToActingStaffId")
    protected fun createdByToActingStaffId(createdBy: String): String {
        return when (createdBy) {
            "system" -> "EROP"
            else -> createdBy
        }
    }
}
