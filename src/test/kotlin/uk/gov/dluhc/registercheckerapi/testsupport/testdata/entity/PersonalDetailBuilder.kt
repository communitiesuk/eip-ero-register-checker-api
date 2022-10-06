package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.Address
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.LocalDate

fun buildPersonalDetail(
    firstName: String = faker.name().firstName(),
    middleNames: String? = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    dateOfBirth: LocalDate? = faker.date().birthday().toLocalDateTime().toLocalDate(),
    email: String? = faker.internet().emailAddress(),
    phoneNumber: String? = faker.phoneNumber().cellPhone(),
    address: Address = buildAddress(),
) = PersonalDetail(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    dateOfBirth = dateOfBirth,
    email = email,
    phoneNumber = phoneNumber,
    address = address
)

fun buildPersonalDetailWithOptionalFieldsAsNull(
    firstName: String = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    address: Address = buildAddressWithOptionalFieldsAsNull(),
) = buildPersonalDetail(
    firstName = firstName,
    surname = surname,
    address = address,
    middleNames = null,
    dateOfBirth = null,
    email = null,
    phoneNumber = null
)
