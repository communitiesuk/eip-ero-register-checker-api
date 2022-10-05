package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildAddressDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildAddress
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetail

internal class PersonalDetailMapperTest {

    private val mapper = PersonalDetailMapperImpl()

    companion object {
        val JPA_MANAGED_FIELDS = arrayOf(
            "id",
            "dateCreated",
        )
    }

    @Nested
    inner class FromDtoToEntity {

        @Test
        fun `should map dto to entity`() {
            // Given
            val personalDetailDto = buildPersonalDetailDto()
            val expected = buildPersonalDetail(
                firstName = personalDetailDto.firstName,
                middleNames = personalDetailDto.middleNames,
                surname = personalDetailDto.surname,
                dateOfBirth = personalDetailDto.dateOfBirth,
                email = personalDetailDto.email,
                phoneNumber = personalDetailDto.phone,
                address = buildAddress(
                    street = personalDetailDto.address.street,
                    property = personalDetailDto.address.property,
                    locality = personalDetailDto.address.locality,
                    town = personalDetailDto.address.town,
                    area = personalDetailDto.address.area,
                    postcode = personalDetailDto.address.postcode,
                    uprn = personalDetailDto.address.uprn
                )
            )

            // When
            val actual = mapper.personalDetailDtoToPersonalDetailEntity(personalDetailDto)

            // Then
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(*JPA_MANAGED_FIELDS)
                .isEqualTo(expected)
        }

        @Test
        fun `should map dto to entity when optional fields are null`() {
            // Given
            val personalDetailDto = buildPersonalDetailDto(
                middleNames = null, dateOfBirth = null, email = null, phone = null,
                address = buildAddressDto(property = null, locality = null, town = null, area = null, uprn = null)
            )

            val expected = buildPersonalDetail(
                firstName = personalDetailDto.firstName,
                middleNames = null,
                surname = personalDetailDto.surname,
                dateOfBirth = null,
                email = null,
                phoneNumber = null,
                address = buildAddress(
                    street = personalDetailDto.address.street,
                    property = null,
                    locality = null,
                    town = null,
                    area = null,
                    postcode = personalDetailDto.address.postcode,
                    uprn = null
                )
            )

            // When
            val actual = mapper.personalDetailDtoToPersonalDetailEntity(personalDetailDto)

            // Then
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(*JPA_MANAGED_FIELDS)
                .isEqualTo(expected)
        }
    }

    @Nested
    inner class FromEntityToDto {

        @Test
        fun `should map entity to dto`() {
            // Given
            val personalDetailEntity = buildPersonalDetail()
            val expected = buildPersonalDetailDto(
                firstName = personalDetailEntity.firstName,
                middleNames = personalDetailEntity.middleNames,
                surname = personalDetailEntity.surname,
                dateOfBirth = personalDetailEntity.dateOfBirth,
                email = personalDetailEntity.email,
                phone = personalDetailEntity.phoneNumber,
                address = buildAddressDto(
                    street = personalDetailEntity.address.street,
                    property = personalDetailEntity.address.property,
                    locality = personalDetailEntity.address.locality,
                    town = personalDetailEntity.address.town,
                    area = personalDetailEntity.address.area,
                    postcode = personalDetailEntity.address.postcode,
                    uprn = personalDetailEntity.address.uprn
                )
            )

            // When
            val actual = mapper.personalDetailEntityToPersonalDetailDto(personalDetailEntity)

            // Then
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(*JPA_MANAGED_FIELDS)
                .isEqualTo(expected)
        }

        @Test
        fun `should map entity to dto when optional fields are null`() {
            // Given
            val personalDetailEntity = buildPersonalDetail(
                middleNames = null, dateOfBirth = null, email = null, phoneNumber = null,
                address = buildAddress(property = null, locality = null, town = null, area = null, uprn = null)
            )
            val expected = buildPersonalDetailDto(
                firstName = personalDetailEntity.firstName,
                middleNames = null,
                surname = personalDetailEntity.surname,
                dateOfBirth = null,
                email = null,
                phone = null,
                address = buildAddressDto(
                    street = personalDetailEntity.address.street,
                    property = null,
                    locality = null,
                    town = null,
                    area = null,
                    postcode = personalDetailEntity.address.postcode,
                    uprn = null
                )
            )

            // When
            val actual = mapper.personalDetailEntityToPersonalDetailDto(personalDetailEntity)

            // Then
            assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected)
        }
    }
}
