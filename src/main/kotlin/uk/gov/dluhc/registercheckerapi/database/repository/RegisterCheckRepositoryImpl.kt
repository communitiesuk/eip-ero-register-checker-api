package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
class RegisterCheckRepositoryImpl(@PersistenceContext val entityManager: EntityManager) : CustomRegisterCheckRepository {

    override fun findPendingEntriesByGssCode(gssCode: String, limit: Int): List<RegisterCheck> {
        val query = """SELECT rc FROM RegisterCheck rc 
               JOIN FETCH rc.personalDetail pd 
               JOIN FETCH pd.address a 
               WHERE rc.status = 'PENDING' AND rc.gssCode = :gssCode 
               ORDER BY rc.dateCreated"""
        return entityManager.createQuery(query, RegisterCheck::class.java)
            .setParameter("gssCode", gssCode)
            .setMaxResults(limit)
            .resultList
    }
}
