package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.registercheckerapi.database.entity.Address
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker

fun buildAddress(
    street: String = faker.address().streetName(),
    property: String? = faker.address().buildingNumber(),
    locality: String? = faker.address().streetName(),
    town: String? = faker.address().city(),
    area: String? = faker.address().state(),
    postcode: String = faker.address().postcode(),
    uprn: String? = RandomStringUtils.randomNumeric(12),
) = Address(
    street = street,
    property = property,
    locality = locality,
    town = town,
    area = area,
    postcode = postcode,
    uprn = uprn
)
