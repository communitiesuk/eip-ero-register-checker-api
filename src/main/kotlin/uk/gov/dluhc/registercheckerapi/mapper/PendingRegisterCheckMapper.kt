package uk.gov.dluhc.registercheckerapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage

@Mapper
interface PendingRegisterCheckMapper {

    @Mapping(target = "correlationId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdBy", source = "requestedBy")
    fun initiateRegisterCheckMessageToPendingRegisterCheckDto(initiateRegisterCheckMessage: InitiateRegisterCheckMessage): PendingRegisterCheckDto

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "dateCreated", source = "createdAt")
    fun pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto: PendingRegisterCheckDto): RegisterCheck

    @Mapping(target = "phoneNumber", source = "phone")
    fun personalDetailDtoToPersonalDetailEntity(personalDetailDto: PersonalDetailDto): PersonalDetail
}
