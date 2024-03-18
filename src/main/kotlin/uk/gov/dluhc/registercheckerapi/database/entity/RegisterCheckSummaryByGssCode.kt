package uk.gov.dluhc.registercheckerapi.database.entity

interface RegisterCheckSummaryByGssCode {
    val gssCode: String
    val registerCheckCount: Int
}
