package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.AdminPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.models.AdminPendingRegisterCheck

/**
 * Maps the entity class [RegisterCheck] to/from the corresponding [AdminPendingRegisterCheckDto].
 */
@Mapper(
    uses = [
        InstantMapper::class,
        SourceTypeMapper::class,
    ]
)
abstract class AdminPendingRegisterCheckMapper {

    @Mapping(target = "createdAt", source = "dateCreated")
    abstract fun registerCheckEntityToAdminPendingRegisterCheckDto(registerCheck: RegisterCheck): AdminPendingRegisterCheckDto

    @Mapping(target = "applicationId", source = "sourceReference")
    abstract fun adminPendingRegisterCheckDtoToAdminPendingRegisterCheckModel(pendingRegisterCheckDto: AdminPendingRegisterCheckDto): AdminPendingRegisterCheck
}
