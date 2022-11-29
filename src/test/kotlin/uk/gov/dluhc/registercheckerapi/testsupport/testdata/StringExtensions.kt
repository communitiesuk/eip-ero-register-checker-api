package uk.gov.dluhc.registercheckerapi.testsupport.testdata

fun String.replaceSpacesWith(replacement: String): String = replace(Regex("\\s+"), replacement)
