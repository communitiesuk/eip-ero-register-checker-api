package uk.gov.dluhc.registercheckerapi.messaging.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildInitiateRegisterCheckMessage
import java.util.UUID.randomUUID
import uk.gov.dluhc.registercheckerapi.dto.SourceType as DtoSourceType

@ExtendWith(MockitoExtension::class)
internal class InitiateRegisterCheckMapperTest {

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private val mapper = InitiateRegisterCheckMapperImpl()

    @Test
    fun `should map model to dto`() {
        // Given
        val message = buildInitiateRegisterCheckMessage()
        given(sourceTypeMapper.fromSqsToDtoEnum(any())).willReturn(DtoSourceType.VOTER_CARD)

        val expected = PendingRegisterCheckDto(
            correlationId = randomUUID(),
            sourceType = DtoSourceType.VOTER_CARD,
            sourceReference = message.sourceReference,
            sourceCorrelationId = message.sourceCorrelationId,
            createdBy = "system",
            gssCode = message.gssCode,
            personalDetail = with(message.personalDetail) {
                PersonalDetailDto(
                    firstName = firstName,
                    middleNames = middleNames,
                    surname = surname,
                    dateOfBirth = dateOfBirth,
                    phone = phone,
                    email = email,
                    address = AddressDto(
                        property = address.property,
                        street = address.street,
                        locality = address.locality,
                        town = address.town,
                        area = address.area,
                        postcode = address.postcode,
                        uprn = address.uprn,
                    )
                )
            },
            emsElectorId = message.emsElectorId,
            historicalSearch = message.historicalSearch,
        )

        // When
        val actual = mapper.initiateCheckMessageToPendingRegisterCheckDto(message)

        // Then
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("correlationId")
            .isEqualTo(expected)
        assertThat(actual.correlationId).isNotNull
        assertThat(actual.createdAt).isNull()
        verify(sourceTypeMapper).fromSqsToDtoEnum(SourceType.VOTER_MINUS_CARD)
        verifyNoMoreInteractions(sourceTypeMapper)
    }
}
