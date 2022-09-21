package uk.gov.dluhc.registercheckerapi.testsupport

import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker

fun getRandomEroId() = "${faker.address().city().lowercase()}-city-council"

fun getDifferentRandomEroId(refEroId: String): String {
    var differentEroId = getRandomEroId()
    while (refEroId == differentEroId) {
        differentEroId = getRandomEroId()
    }
    return differentEroId
}

fun getRandomGssCode() = "E${faker.random().nextLong(8)}"
