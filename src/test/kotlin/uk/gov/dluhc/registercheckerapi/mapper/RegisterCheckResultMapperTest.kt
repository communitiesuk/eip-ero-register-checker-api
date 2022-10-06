package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildAddressDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildRegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckMatchRequest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultRequest
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckResultMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var personalDetailMapper: PersonalDetailMapper

    @InjectMocks
    private val mapper = RegisterCheckResultMapperImpl()

    companion object {
        val JPA_MANAGED_FIELDS = arrayOf(
            "id",
            "dateCreated",
        )
    }

    @Nested
    inner class FromRegisterCheckResultRequestApiToDto {

        @ParameterizedTest
        @CsvSource(
            value = [
                "0,   NO_MATCH",
                "1,   EXACT_MATCH",
                "2,   MULTIPLE_MATCH",
                "3,   MULTIPLE_MATCH",
                "4,   MULTIPLE_MATCH",
                "5,   MULTIPLE_MATCH",
                "6,   MULTIPLE_MATCH",
                "7,   MULTIPLE_MATCH",
                "8,   MULTIPLE_MATCH",
                "9,   MULTIPLE_MATCH",
                "10,  MULTIPLE_MATCH",
                "11,  TOO_MANY_MATCHES",
                "100, TOO_MANY_MATCHES",
            ]
        )
        fun `should map api to dto for a given registerCheckMatchCount`(
            givenRegisterCheckMatchCount: Int,
            expectedRegisterCheckStatus: RegisterCheckStatus
        ) {
            // Given
            val queryParamRequestId = UUID.randomUUID()
            val createdAt = OffsetDateTime.now()
            val applicationCreatedAt = OffsetDateTime.now().minusDays(5)
            val apiRequest = buildRegisterCheckResultRequest(
                createdAt = createdAt,
                applicationCreatedAt = applicationCreatedAt,
                registerCheckMatchCount = givenRegisterCheckMatchCount
            )
            val expectedMatchSentAt = createdAt.toInstant()
            val expectedApplicationCreatedAt = applicationCreatedAt.toInstant()

            given(instantMapper.toInstant(eq(createdAt))).willReturn(createdAt.toInstant())
            given(instantMapper.toInstant(eq(applicationCreatedAt))).willReturn(expectedApplicationCreatedAt)

            val expected = RegisterCheckResultDto(
                requestId = queryParamRequestId,
                correlationId = apiRequest.requestid,
                gssCode = apiRequest.gssCode,
                matchResultSentAt = expectedMatchSentAt,
                matchCount = apiRequest.registerCheckMatchCount,
                registerCheckStatus = expectedRegisterCheckStatus,
                registerCheckMatches = apiRequest.registerCheckMatches?.map {
                    toRegisterCheckMapDtoFromApi(it, expectedApplicationCreatedAt)
                }
            )

            // When
            val actual = mapper.fromRegisterCheckResultRequestApiToDto(queryParamRequestId, apiRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            assertThat(actual.registerCheckStatus).isEqualTo(expectedRegisterCheckStatus)
            verify(instantMapper).toInstant(createdAt)
            verify(instantMapper).toInstant(applicationCreatedAt)
        }

        @Test
        fun `should map api to dto when optional fields are null`() {
            // Given
            val queryParamRequestId = UUID.randomUUID()
            val createdAt = OffsetDateTime.now()
            val apiRequest = buildRegisterCheckResultRequest(
                createdAt = createdAt,
                registerCheckMatchCount = 0,
                applicationCreatedAt = null,
                registerCheckMatches = null
            )

            val expectedMatchSentAt = createdAt.toInstant()
            given(instantMapper.toInstant(eq(createdAt))).willReturn(createdAt.toInstant())

            val expected = RegisterCheckResultDto(
                requestId = queryParamRequestId,
                correlationId = apiRequest.requestid,
                gssCode = apiRequest.gssCode,
                matchResultSentAt = expectedMatchSentAt,
                matchCount = apiRequest.registerCheckMatchCount,
                registerCheckStatus = RegisterCheckStatus.NO_MATCH,
                registerCheckMatches = null
            )

            // When
            val actual = mapper.fromRegisterCheckResultRequestApiToDto(queryParamRequestId, apiRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            assertThat(actual.registerCheckStatus).isEqualTo(RegisterCheckStatus.NO_MATCH)
            verify(instantMapper).toInstant(createdAt)
        }
    }

    @Nested
    inner class FromRegisterCheckMatchApiToDto {

        @Test
        fun `should map api to dto`() {
            // Given
            val applicationCreatedAt = OffsetDateTime.now().minusDays(5)
            val apiRequest = buildRegisterCheckMatchRequest(applicationCreatedAt = applicationCreatedAt)

            val expectedApplicationCreatedAt = applicationCreatedAt.toInstant()
            given(instantMapper.toInstant(any())).willReturn(expectedApplicationCreatedAt)
            val expected = toRegisterCheckMapDtoFromApi(apiRequest, expectedApplicationCreatedAt)

            // When
            val actual = mapper.fromRegisterCheckMatchApiToDto(apiRequest)

            // Then
            assertThat(actual).isEqualTo(expected)
            verify(instantMapper).toInstant(applicationCreatedAt)
            verifyNoInteractions(personalDetailMapper)
        }

        @Test
        fun `should map api to dto when optional fields are null`() {
            // Given
            val apiRequest = buildRegisterCheckMatchRequest(
                mn = null,
                dob = null,
                regproperty = null,
                reglocality = null,
                regtown = null,
                regarea = null,
                reguprn = null,
                phone = null,
                email = null,
                registeredStartDate = null,
                registeredEndDate = null,
                applicationCreatedAt = null
            )

            val expected = toRegisterCheckMapDtoFromApi(apiRequest, null)

            // When
            val actual = mapper.fromRegisterCheckMatchApiToDto(apiRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verifyNoInteractions(personalDetailMapper)
        }
    }

    @Nested
    inner class FromDtoToRegisterCheckMatchEntity {

        @Test
        fun `should map dto to entity`() {
            // Given
            val registerCheckMatchDto = buildRegisterCheckMatchDto()
            val expectedPersonalDetailEntity = buildPersonalDetail()

            given(personalDetailMapper.personalDetailDtoToPersonalDetailEntity(any())).willReturn(expectedPersonalDetailEntity)
            val expected = buildRegisterCheckMatch(
                emsElectorId = registerCheckMatchDto.emsElectorId,
                attestationCount = registerCheckMatchDto.attestationCount,
                personalDetail = expectedPersonalDetailEntity,
                registeredStartDate = registerCheckMatchDto.registeredStartDate,
                registeredEndDate = registerCheckMatchDto.registeredEndDate,
                applicationCreatedAt = registerCheckMatchDto.applicationCreatedAt!!,
                franchiseCode = registerCheckMatchDto.franchiseCode
            )

            // When
            val actual = mapper.fromDtoToRegisterCheckMatchEntity(registerCheckMatchDto)

            // Then
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(*JPA_MANAGED_FIELDS)
                .isEqualTo(expected)
            verify(personalDetailMapper).personalDetailDtoToPersonalDetailEntity(registerCheckMatchDto.personalDetail)
            verifyNoMoreInteractions(personalDetailMapper)
        }
    }

    private fun toRegisterCheckMapDtoFromApi(
        rcmApi: RegisterCheckMatch,
        expectedApplicationCreatedAt: Instant?
    ): RegisterCheckMatchDto {
        return RegisterCheckMatchDto(
            emsElectorId = rcmApi.emsElectorId,
            attestationCount = rcmApi.attestationCount,
            personalDetail = buildPersonalDetailDto(
                firstName = rcmApi.fn,
                middleNames = rcmApi.mn,
                surname = rcmApi.ln,
                dateOfBirth = rcmApi.dob,
                email = rcmApi.email,
                phone = rcmApi.phone,
                address = buildAddressDto(
                    street = rcmApi.regstreet,
                    property = rcmApi.regproperty,
                    locality = rcmApi.reglocality,
                    town = rcmApi.regtown,
                    area = rcmApi.regarea,
                    postcode = rcmApi.regpostcode,
                    uprn = rcmApi.reguprn
                )
            ),
            registeredStartDate = rcmApi.registeredStartDate,
            registeredEndDate = rcmApi.registeredEndDate,
            applicationCreatedAt = expectedApplicationCreatedAt,
            franchiseCode = rcmApi.franchiseCode,
        )
    }
}
