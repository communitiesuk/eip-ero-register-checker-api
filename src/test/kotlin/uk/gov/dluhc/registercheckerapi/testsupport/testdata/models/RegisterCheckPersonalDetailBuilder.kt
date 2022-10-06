package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import net.datafaker.Address
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckAddress
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker
import java.time.LocalDate

fun buildRegisterCheckPersonalDetail(
    firstName: String = DataFaker.faker.name().firstName(),
    middleNames: String? = DataFaker.faker.name().firstName(),
    surname: String = DataFaker.faker.name().lastName(),
    dateOfBirth: LocalDate? = DataFaker.faker.date().birthday().toLocalDateTime().toLocalDate(),
    email: String? = DataFaker.faker.internet().emailAddress(),
    phone: String? = DataFaker.faker.phoneNumber().cellPhone(),
    address: RegisterCheckAddress = buildRegisterCheckAddress()
) = RegisterCheckPersonalDetail(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    dateOfBirth = dateOfBirth,
    phone = phone,
    email = email,
    address = address
)

fun buildRegisterCheckAddress(
    fakeAddress: Address = DataFaker.faker.address(),
    property: String? = fakeAddress.buildingNumber(),
    street: String = fakeAddress.streetName(),
    locality: String? = fakeAddress.streetName(),
    town: String? = fakeAddress.city(),
    area: String? = fakeAddress.state(),
    postcode: String = fakeAddress.postcode(),
    uprn: String? = RandomStringUtils.randomNumeric(12),
) = RegisterCheckAddress(
    property = property,
    street = street,
    locality = locality,
    town = town,
    area = area,
    postcode = postcode,
    uprn = uprn,
)

fun buildRegisterCheckPersonalDetailFromMatchModel(match: uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch): RegisterCheckPersonalDetail =
    with(match) {
        RegisterCheckPersonalDetail(
            firstName = fn,
            middleNames = mn,
            surname = ln,
            dateOfBirth = dob,
            phone = phone,
            email = email,
            address = buildRegisterCheckAddress(match)
        )
    }

fun buildRegisterCheckAddress(match: uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch) =
    with(match) {
        RegisterCheckAddress(
            property = regproperty,
            street = regstreet,
            locality = reglocality,
            town = regtown,
            area = regarea,
            postcode = regpostcode,
            uprn = reguprn,
        )
    }

fun buildRegisterCheckPersonalDetailFromMatchDto(match: RegisterCheckMatchDto): RegisterCheckPersonalDetail =
    with(match.personalDetail) {
        RegisterCheckPersonalDetail(
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            dateOfBirth = dateOfBirth,
            phone = phone,
            email = email,
            address = buildRegisterCheckAddress(address)
        )
    }

fun buildRegisterCheckAddress(address: AddressDto): RegisterCheckAddress =
    with(address) {
        RegisterCheckAddress(
            property = property,
            street = street,
            locality = locality,
            town = town,
            area = area,
            postcode = postcode,
            uprn = uprn,
        )
    }
