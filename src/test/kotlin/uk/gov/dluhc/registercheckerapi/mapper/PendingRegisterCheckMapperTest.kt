package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.database.entity.Address
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildInitiateRegisterCheckMessage
import java.util.UUID
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as EntitySourceType
import uk.gov.dluhc.registercheckerapi.dto.SourceType as DtoSourceType

internal class PendingRegisterCheckMapperTest {

    private val mapper = PendingRegisterCheckMapperImpl()

    @Test
    fun `should map model to dto`() {
        // Given
        val message = buildInitiateRegisterCheckMessage()
        val expected = PendingRegisterCheckDto(
            correlationId = UUID.randomUUID(),
            sourceType = DtoSourceType.VOTER_CARD,
            sourceReference = message.sourceReference,
            sourceCorrelationId = message.sourceCorrelationId,
            createdBy = "system",
            gssCode = message.gssCode,
            personalDetail = PersonalDetailDto(
                firstName = message.personalDetail.firstName,
                middleNames = message.personalDetail.middleNames,
                surname = message.personalDetail.surname,
                dateOfBirth = message.personalDetail.dateOfBirth,
                phone = message.personalDetail.phone,
                email = message.personalDetail.email,
                address = AddressDto(
                    property = message.personalDetail.address.property,
                    street = message.personalDetail.address.street,
                    locality = message.personalDetail.address.locality,
                    town = message.personalDetail.address.town,
                    area = message.personalDetail.address.area,
                    postcode = message.personalDetail.address.postcode,
                    uprn = message.personalDetail.address.uprn,
                )
            )
        )

        // When
        val actual = mapper.initiateRegisterCheckMessageToPendingRegisterCheckDto(message)

        // Then
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("correlationId")
            .isEqualTo(expected)
        assertThat(actual.correlationId).isNotNull
        assertThat(actual.createdAt).isNull()
    }

    @Test
    fun `should map dto to entity`() {
        // Given
        val pendingRegisterCheckDto = buildPendingRegisterCheckDto()
        val expected = RegisterCheck(
            id = UUID.randomUUID(),
            correlationId = pendingRegisterCheckDto.correlationId,
            sourceType = EntitySourceType.VOTER_CARD,
            sourceReference = pendingRegisterCheckDto.sourceReference,
            sourceCorrelationId = pendingRegisterCheckDto.sourceCorrelationId,
            createdBy = pendingRegisterCheckDto.createdBy,
            gssCode = pendingRegisterCheckDto.gssCode,
            status = CheckStatus.PENDING,
            personalDetail = PersonalDetail(
                firstName = pendingRegisterCheckDto.personalDetail.firstName,
                middleNames = pendingRegisterCheckDto.personalDetail.middleNames,
                surname = pendingRegisterCheckDto.personalDetail.surname,
                dateOfBirth = pendingRegisterCheckDto.personalDetail.dateOfBirth,
                phoneNumber = pendingRegisterCheckDto.personalDetail.phone,
                email = pendingRegisterCheckDto.personalDetail.email,
                address = Address(
                    property = pendingRegisterCheckDto.personalDetail.address.property,
                    street = pendingRegisterCheckDto.personalDetail.address.street,
                    locality = pendingRegisterCheckDto.personalDetail.address.locality,
                    town = pendingRegisterCheckDto.personalDetail.address.town,
                    area = pendingRegisterCheckDto.personalDetail.address.area,
                    postcode = pendingRegisterCheckDto.personalDetail.address.postcode,
                    uprn = pendingRegisterCheckDto.personalDetail.address.uprn,
                )
            )
        )

        // When
        val actual = mapper.pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)

        // Then
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("id", "status")
            .isEqualTo(expected)
        assertThat(actual.id).isNotNull
        assertThat(actual.status).isEqualTo(CheckStatus.PENDING)
        assertThat(actual.dateCreated).isNull()
    }

    @Test
    fun `should map entity to dto`() {
        // Given
        val registerCheckEntity = buildRegisterCheck()
        val expected = PendingRegisterCheckDto(
            correlationId = registerCheckEntity.correlationId,
            sourceType = DtoSourceType.VOTER_CARD,
            sourceReference = registerCheckEntity.sourceReference,
            sourceCorrelationId = registerCheckEntity.sourceCorrelationId,
            createdBy = registerCheckEntity.createdBy,
            gssCode = registerCheckEntity.gssCode,
            createdAt = registerCheckEntity.dateCreated,
            personalDetail = PersonalDetailDto(
                firstName = registerCheckEntity.personalDetail.firstName,
                middleNames = registerCheckEntity.personalDetail.middleNames,
                surname = registerCheckEntity.personalDetail.surname,
                dateOfBirth = registerCheckEntity.personalDetail.dateOfBirth,
                phone = registerCheckEntity.personalDetail.phoneNumber,
                email = registerCheckEntity.personalDetail.email,
                address = AddressDto(
                    property = registerCheckEntity.personalDetail.address.property,
                    street = registerCheckEntity.personalDetail.address.street,
                    locality = registerCheckEntity.personalDetail.address.locality,
                    town = registerCheckEntity.personalDetail.address.town,
                    area = registerCheckEntity.personalDetail.address.area,
                    postcode = registerCheckEntity.personalDetail.address.postcode,
                    uprn = registerCheckEntity.personalDetail.address.uprn,
                )
            )
        )

        // When
        val actual = mapper.registerCheckEntityToPendingRegisterCheckDto(registerCheckEntity)

        // Then
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }
}
