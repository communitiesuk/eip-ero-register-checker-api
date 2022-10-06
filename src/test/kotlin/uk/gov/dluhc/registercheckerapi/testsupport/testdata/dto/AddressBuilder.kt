package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker

fun buildAddressDto(
    street: String = faker.address().streetName(),
    property: String? = faker.address().buildingNumber(),
    locality: String? = faker.address().streetName(),
    town: String? = faker.address().city(),
    area: String? = faker.address().state(),
    postcode: String = faker.address().postcode(),
    uprn: String? = RandomStringUtils.randomNumeric(12)
) = AddressDto(
    street = street,
    property = property,
    locality = locality,
    town = town,
    area = area,
    postcode = postcode,
    uprn = uprn
)

fun buildAddressDtoWithOptionalFieldsNull(
    street: String = faker.address().streetName(),
    postcode: String = faker.address().postcode()
) = buildAddressDto(
    street = street,
    postcode = postcode,
    property = null,
    locality = null,
    town = null,
    area = null,
    uprn = null
)
