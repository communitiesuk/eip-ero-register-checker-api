package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckResultDto
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildAddressDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultRequest
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckResultMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @InjectMocks
    private val mapper = RegisterCheckResultMapperImpl()

    @Nested
    inner class FromRegisterCheckResultRequestApiToDto {

        @Test
        fun `should map api to dto`() {
            // Given
            val queryParamRequestId = UUID.randomUUID().toString()
            val createdAt = OffsetDateTime.now()
            val applicationCreatedAt = OffsetDateTime.now().minusDays(5)
            val apiRequest = buildRegisterCheckResultRequest(createdAt = createdAt, applicationCreatedAt = applicationCreatedAt)

            val expectedMatchSentAt = createdAt.toInstant()
            val expectedApplicationCreatedAt = applicationCreatedAt.toInstant()

            given(instantMapper.toInstant(eq(createdAt))).willReturn(createdAt.toInstant())
            given(instantMapper.toInstant(eq(applicationCreatedAt))).willReturn(expectedApplicationCreatedAt)

            val expected = RegisterCheckResultDto(
                requestId = UUID.fromString(queryParamRequestId),
                correlationId = apiRequest.requestid,
                gssCode = apiRequest.gssCode,
                matchResultSentAt = expectedMatchSentAt,
                matchCount = apiRequest.registerCheckMatchCount,
                registerCheckMatchDto = apiRequest.registerCheckMatches?.map {
                    toRegisterCheckMapDtoFromApi(it, expectedApplicationCreatedAt)
                }
            )

            // When
            val actual = mapper.fromRegisterCheckResultRequestApiToDto(queryParamRequestId, apiRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(instantMapper).toInstant(createdAt)
            verify(instantMapper).toInstant(applicationCreatedAt)
        }

        @Test
        fun `should map api to dto when optional fields are null`() {
            // Given
            val queryParamRequestId = UUID.randomUUID().toString()
            val createdAt = OffsetDateTime.now()
            val apiRequest = buildRegisterCheckResultRequest(
                createdAt = createdAt,
                applicationCreatedAt = null,
                registerCheckMatches = null
            )

            val expectedMatchSentAt = createdAt.toInstant()
            given(instantMapper.toInstant(eq(createdAt))).willReturn(createdAt.toInstant())

            val expected = RegisterCheckResultDto(
                requestId = UUID.fromString(queryParamRequestId),
                correlationId = apiRequest.requestid,
                gssCode = apiRequest.gssCode,
                matchResultSentAt = expectedMatchSentAt,
                matchCount = apiRequest.registerCheckMatchCount,
                registerCheckMatchDto = null
            )

            // When
            val actual = mapper.fromRegisterCheckResultRequestApiToDto(queryParamRequestId, apiRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(instantMapper).toInstant(createdAt)
        }
    }

    @Nested
    inner class FromRegisterCheckMatchApiToDto {

        @Test
        fun `should map api to dto`() {
            // Given
            val applicationCreatedAt = OffsetDateTime.now().minusDays(5)
            val apiRequest = buildRegisterCheckMatch(applicationCreatedAt = applicationCreatedAt)

            val expectedApplicationCreatedAt = applicationCreatedAt.toInstant()
            given(instantMapper.toInstant(any())).willReturn(expectedApplicationCreatedAt)
            val expected = toRegisterCheckMapDtoFromApi(apiRequest, expectedApplicationCreatedAt)

            // When
            val actual = mapper.fromRegisterCheckMatchApiToDto(apiRequest)

            // Then
            assertThat(actual).isEqualTo(expected)
            verify(instantMapper).toInstant(applicationCreatedAt)
        }

        @Test
        fun `should map api to dto when optional fields are null`() {
            // Given
            val apiRequest = buildRegisterCheckMatch(
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
                applicationCreatedAt = null,
                franchiseCode = null,
            )

            val expected = toRegisterCheckMapDtoFromApi(apiRequest, null)

            // When
            val actual = mapper.fromRegisterCheckMatchApiToDto(apiRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
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
