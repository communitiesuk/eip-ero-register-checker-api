package uk.gov.dluhc.registercheckerapi.database.repository

import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck

interface CustomRegisterCheckRepository {

    fun findPendingEntriesByGssCode(gssCode: String, limit: Int = 100): List<RegisterCheck>
}
