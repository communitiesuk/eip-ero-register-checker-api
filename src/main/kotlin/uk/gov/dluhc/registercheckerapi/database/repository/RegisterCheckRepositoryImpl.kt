package uk.gov.dluhc.registercheckerapi.database.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck

@Repository
class RegisterCheckRepositoryImpl(@PersistenceContext val entityManager: EntityManager) : CustomRegisterCheckRepository {

    override fun findPendingEntriesByGssCodes(gssCodes: List<String>, limit: Int): List<RegisterCheck> {
        val query = """SELECT rc FROM RegisterCheck rc 
               JOIN FETCH rc.personalDetail pd 
               JOIN FETCH pd.address a 
               WHERE rc.status = 'PENDING' AND rc.gssCode IN (:gssCodes) 
               ORDER BY rc.dateCreated"""
        return entityManager.createQuery(query, RegisterCheck::class.java)
            .setParameter("gssCodes", gssCodes)
            .setMaxResults(limit)
            .resultList
    }

    override fun adminFindPendingEntriesByGssCodes(gssCodes: List<String>, limit: Int): List<RegisterCheck> {
        val query = """SELECT rc FROM RegisterCheck rc
            WHERE rc.status = 'PENDING' AND rc.gssCode IN (:gssCodes)
            ORDER BY rc.dateCreated
        """
        return entityManager.createQuery(query, RegisterCheck::class.java)
            .setParameter("gssCodes", gssCodes)
            .setMaxResults(limit)
            .resultList
    }
}
