package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.PersonalDetailDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.LocalDate

fun buildPersonalDetailDto(
    firstName: String = faker.name().firstName(),
    middleNames: String? = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    dateOfBirth: LocalDate? = faker.date().birthday().toLocalDateTime().toLocalDate(),
    email: String? = faker.internet().emailAddress(),
    phone: String? = faker.phoneNumber().cellPhone(),
    address: AddressDto = buildAddressDto()
) = PersonalDetailDto(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    dateOfBirth = dateOfBirth,
    email = email,
    phone = phone,
    address = address
)

fun buildPersonalDetailDtoWithOptionalFieldsNull(
    firstName: String = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    address: AddressDto = buildAddressDtoWithOptionalFieldsNull()
) = buildPersonalDetailDto(
    firstName = firstName,
    surname = surname,
    address = address,
    middleNames = null,
    dateOfBirth = null,
    email = null,
    phone = null
)
