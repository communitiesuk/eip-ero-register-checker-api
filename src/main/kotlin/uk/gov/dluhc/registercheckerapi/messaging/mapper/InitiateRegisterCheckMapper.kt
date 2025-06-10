package uk.gov.dluhc.registercheckerapi.messaging.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckForwardingMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import java.util.UUID

/**
 * Maps incoming [InitiateRegisterCheckMessage] to [PendingRegisterCheckDto].
 */
@Mapper(
    uses = [SourceTypeMapper::class],
    imports = [UUID::class]
)
interface InitiateRegisterCheckMapper {

    @Mapping(target = "correlationId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdBy", source = "requestedBy")
    fun initiateCheckMessageToPendingRegisterCheckDto(initiateRegisterCheckMessage: InitiateRegisterCheckMessage): PendingRegisterCheckDto

    fun initiateCheckToInitiateCheckForwardingMessage(initiateRegisterCheckMessage: InitiateRegisterCheckMessage, correlationId: UUID): InitiateRegisterCheckForwardingMessage
}
