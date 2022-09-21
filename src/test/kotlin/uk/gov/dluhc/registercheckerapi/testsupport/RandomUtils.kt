package uk.gov.dluhc.registercheckerapi.testsupport

import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker

fun getRandomEroId() = "${faker.address().city().lowercase()}-city-council"

fun getDifferentRandomEroId(refEroId: String): String {
    var differentEroId = getRandomEroId()
    while (refEroId == differentEroId) {
        differentEroId = getRandomEroId()
    }
    return differentEroId
}

fun getRandomGssCode() = "E${randomNumeric(8)}"
