package uk.gov.dluhc.registercheckerapi.config.database

import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionException
import org.springframework.transaction.TransactionStatus

/**
 * The purpose of this class is to wrap the (final) JpaTransactionManager.
 * This is to allow the type of transaction (read-only or read-write) to be set on the data source for this thread,
 * so that the appropriate instances are used on the cluster.
 */
class ReplicaAwareTransactionManager(
    private val wrapped: JpaTransactionManager,
    private val datasource: TransactionRoutingDataSource
) : PlatformTransactionManager {

    @Throws(TransactionException::class)
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
        datasource.setReadonlyDataSource(definition != null && definition.isReadOnly)
        return wrapped.getTransaction(definition)
    }

    @Throws(TransactionException::class)
    override fun commit(status: TransactionStatus) {
        wrapped.commit(status)
    }

    @Throws(TransactionException::class)
    override fun rollback(status: TransactionStatus) {
        wrapped.rollback(status)
    }
}
