package uk.gov.dluhc.registercheckerapi.config.database

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import uk.gov.dluhc.registercheckerapi.config.database.TransactionRoutingDataSource.DataSourceType.READ_ONLY
import uk.gov.dluhc.registercheckerapi.config.database.TransactionRoutingDataSource.DataSourceType.READ_WRITE
import javax.sql.DataSource

/**
 * This DataSource wraps a read-only and a read-write DataSource.
 * The appropriate data source is used for the type of transaction this thread is currently handling.
 */
class TransactionRoutingDataSource(readWriteDataSource: DataSource, readOnlyDataSource: DataSource) :
    AbstractRoutingDataSource() {

    private val currentDataSource = ThreadLocal<DataSourceType>().apply { this.set(READ_WRITE) }

    init {
        val dataSources: MutableMap<Any, Any> = HashMap()
        dataSources[READ_WRITE] = readWriteDataSource
        dataSources[READ_ONLY] = readOnlyDataSource

        setTargetDataSources(dataSources)
        setDefaultTargetDataSource(readWriteDataSource)
    }

    fun setReadonlyDataSource(isReadonly: Boolean) {
        currentDataSource.set(if (isReadonly) READ_ONLY else READ_WRITE)
    }

    override fun determineCurrentLookupKey(): Any? = currentDataSource.get()

    private enum class DataSourceType {
        READ_ONLY, READ_WRITE
    }
}
