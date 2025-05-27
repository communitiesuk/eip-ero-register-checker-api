package uk.gov.dluhc.registercheckerapi.testsupport

import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.replaceSpacesWith

fun getRandomEroId() = "${faker.address().city().lowercase()}-city-council"

fun getRandomEroName() = "${faker.address().city()} City Council"

fun getRandomLocalAuthorityName() = "${faker.address().city()} City Council"

fun getRandomEmailAddress(): String = "contact@${getRandomEroId().replaceSpacesWith("-")}.gov.uk"

fun getRandomWebsiteAddress(): String = "https://www.${getRandomEroId().replaceSpacesWith("-")}.gov.uk"

fun getRandomPhoneNumber(): String = faker.phoneNumber().cellPhone()

fun getRandomIpV4Address(): String = faker.internet().ipV4Address()

fun getDifferentRandomEroId(refEroId: String): String {
    var differentEroId = getRandomEroId()
    while (refEroId == differentEroId) {
        differentEroId = getRandomEroId()
    }
    return differentEroId
}

fun getRandomGssCode() = "E${randomNumeric(8)}"
