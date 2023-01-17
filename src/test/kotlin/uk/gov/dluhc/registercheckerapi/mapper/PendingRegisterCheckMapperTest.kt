package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterCheck
import uk.gov.dluhc.registercheckerapi.models.SourceSystem
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildInitiateRegisterCheckMessage
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID.randomUUID
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as EntitySourceType
import uk.gov.dluhc.registercheckerapi.dto.SourceType as DtoSourceType

@ExtendWith(MockitoExtension::class)
internal class PendingRegisterCheckMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var personalDetailMapper: PersonalDetailMapper

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private val mapper = PendingRegisterCheckMapperImpl()

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
        verify(sourceTypeMapper).fromSqsToDtoEnum(SourceType.VOTER_MINUS_CARD)
        verifyNoInteractions(instantMapper, personalDetailMapper)
        verifyNoMoreInteractions(sourceTypeMapper)
    }

    @Test
    fun `should map dto to entity`() {
        // Given
        val pendingRegisterCheckDto = buildPendingRegisterCheckDto()
        val expectedPersonalDetailEntity = buildPersonalDetail()
        val expectedSourceType = EntitySourceType.VOTER_CARD
        given(personalDetailMapper.personalDetailDtoToPersonalDetailEntity(any())).willReturn(expectedPersonalDetailEntity)
        given(sourceTypeMapper.fromDtoToEntityEnum(any())).willReturn(expectedSourceType)

        val expected = RegisterCheck(
            correlationId = pendingRegisterCheckDto.correlationId,
            sourceType = expectedSourceType,
            sourceReference = pendingRegisterCheckDto.sourceReference,
            sourceCorrelationId = pendingRegisterCheckDto.sourceCorrelationId,
            createdBy = pendingRegisterCheckDto.createdBy,
            gssCode = pendingRegisterCheckDto.gssCode,
            status = CheckStatus.PENDING,
            personalDetail = expectedPersonalDetailEntity
        )

        // When
        val actual = mapper.pendingRegisterCheckDtoToRegisterCheckEntity(pendingRegisterCheckDto)

        // Then
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("status")
            .isEqualTo(expected)
        assertThat(actual.status).isEqualTo(CheckStatus.PENDING)
        assertThat(actual.dateCreated).isNull()
        verify(personalDetailMapper).personalDetailDtoToPersonalDetailEntity(pendingRegisterCheckDto.personalDetail)
        verify(sourceTypeMapper).fromDtoToEntityEnum(DtoSourceType.VOTER_CARD)
        verifyNoMoreInteractions(personalDetailMapper)
        verifyNoInteractions(instantMapper)
    }

    @Test
    fun `should map entity to dto`() {
        // Given
        val registerCheckEntity = buildRegisterCheck()
        val expectedPersonalDetailDto = buildPersonalDetailDto()
        given(personalDetailMapper.personalDetailEntityToPersonalDetailDto(any())).willReturn(expectedPersonalDetailDto)
        given(sourceTypeMapper.fromEntityToDtoEnum(any())).willReturn(DtoSourceType.VOTER_CARD)

        val expected = PendingRegisterCheckDto(
            correlationId = registerCheckEntity.correlationId,
            sourceType = DtoSourceType.VOTER_CARD,
            sourceReference = registerCheckEntity.sourceReference,
            sourceCorrelationId = registerCheckEntity.sourceCorrelationId,
            createdBy = registerCheckEntity.createdBy,
            gssCode = registerCheckEntity.gssCode,
            createdAt = registerCheckEntity.dateCreated,
            personalDetail = expectedPersonalDetailDto
        )

        // When
        val actual = mapper.registerCheckEntityToPendingRegisterCheckDto(registerCheckEntity)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(personalDetailMapper).personalDetailEntityToPersonalDetailDto(registerCheckEntity.personalDetail)
        verify(sourceTypeMapper).fromEntityToDtoEnum(EntitySourceType.VOTER_CARD)
        verifyNoMoreInteractions(personalDetailMapper, sourceTypeMapper)
        verifyNoInteractions(instantMapper)
    }

    @Test
    fun `should map dto to model`() {
        // Given
        val createdAt = Instant.now()
        val pendingRegisterCheckDto = buildPendingRegisterCheckDto(createdAt = createdAt)
        given(instantMapper.toOffsetDateTime(any())).willReturn(createdAt.atOffset(ZoneOffset.UTC))
        given(sourceTypeMapper.sourceTypeDtoToSourceSystem(any())).willReturn(SourceSystem.EROP)

        val expected = PendingRegisterCheck(
            requestid = pendingRegisterCheckDto.correlationId,
            source = SourceSystem.EROP,
            gssCode = pendingRegisterCheckDto.gssCode,
            actingStaffId = "EROP",
            createdAt = pendingRegisterCheckDto.createdAt!!.atOffset(ZoneOffset.UTC),
            fn = pendingRegisterCheckDto.personalDetail.firstName,
            mn = pendingRegisterCheckDto.personalDetail.middleNames,
            ln = pendingRegisterCheckDto.personalDetail.surname,
            dob = pendingRegisterCheckDto.personalDetail.dateOfBirth,
            phone = pendingRegisterCheckDto.personalDetail.phone,
            email = pendingRegisterCheckDto.personalDetail.email,
            regstreet = pendingRegisterCheckDto.personalDetail.address.street,
            regpostcode = pendingRegisterCheckDto.personalDetail.address.postcode,
            regproperty = pendingRegisterCheckDto.personalDetail.address.property,
            reglocality = pendingRegisterCheckDto.personalDetail.address.locality,
            regtown = pendingRegisterCheckDto.personalDetail.address.town,
            regarea = pendingRegisterCheckDto.personalDetail.address.area,
            reguprn = pendingRegisterCheckDto.personalDetail.address.uprn
        )

        // When
        val actual = mapper.pendingRegisterCheckDtoToPendingRegisterCheckModel(pendingRegisterCheckDto)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(instantMapper).toOffsetDateTime(createdAt)
        verify(sourceTypeMapper).sourceTypeDtoToSourceSystem(DtoSourceType.VOTER_CARD)
        verifyNoInteractions(personalDetailMapper)
    }

    @Test
    fun `should map manual register check dto to model`() {
        // Given
        val createdAt = Instant.now()
        val pendingRegisterCheckDto = buildPendingRegisterCheckDto(
            createdAt = createdAt,
            createdBy = "joe.bloggs@gmail.com"
        )
        given(instantMapper.toOffsetDateTime(any())).willReturn(createdAt.atOffset(ZoneOffset.UTC))
        given(sourceTypeMapper.sourceTypeDtoToSourceSystem(any())).willReturn(SourceSystem.EROP)

        val expected = PendingRegisterCheck(
            requestid = pendingRegisterCheckDto.correlationId,
            source = SourceSystem.EROP,
            gssCode = pendingRegisterCheckDto.gssCode,
            actingStaffId = "joe.bloggs@gmail.com",
            createdAt = pendingRegisterCheckDto.createdAt!!.atOffset(ZoneOffset.UTC),
            fn = pendingRegisterCheckDto.personalDetail.firstName,
            mn = pendingRegisterCheckDto.personalDetail.middleNames,
            ln = pendingRegisterCheckDto.personalDetail.surname,
            dob = pendingRegisterCheckDto.personalDetail.dateOfBirth,
            phone = pendingRegisterCheckDto.personalDetail.phone,
            email = pendingRegisterCheckDto.personalDetail.email,
            regstreet = pendingRegisterCheckDto.personalDetail.address.street,
            regpostcode = pendingRegisterCheckDto.personalDetail.address.postcode,
            regproperty = pendingRegisterCheckDto.personalDetail.address.property,
            reglocality = pendingRegisterCheckDto.personalDetail.address.locality,
            regtown = pendingRegisterCheckDto.personalDetail.address.town,
            regarea = pendingRegisterCheckDto.personalDetail.address.area,
            reguprn = pendingRegisterCheckDto.personalDetail.address.uprn
        )

        // When
        val actual = mapper.pendingRegisterCheckDtoToPendingRegisterCheckModel(pendingRegisterCheckDto)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(instantMapper).toOffsetDateTime(createdAt)
        verify(sourceTypeMapper).sourceTypeDtoToSourceSystem(DtoSourceType.VOTER_CARD)
        verifyNoInteractions(personalDetailMapper)
    }
}
