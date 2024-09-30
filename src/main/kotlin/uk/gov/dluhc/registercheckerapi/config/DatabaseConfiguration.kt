package uk.gov.dluhc.registercheckerapi.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import uk.gov.dluhc.registercheckerapi.config.database.ReplicaAwareTransactionManager
import uk.gov.dluhc.registercheckerapi.config.database.TransactionRoutingDataSource
import javax.sql.DataSource

/**
 * This config sets up two DB connection pools for two DataSources - one read-only and one read-write, and
 * integrates them into JPA to be used according to the transaction type.
 * This is necessary to use both the reader and writer Aurora instances, as currently the AWS JDBC library
 * doesn't support splitting the queries on type.
 */
@Configuration
class DatabaseConfiguration(
    private val dataSourceProperties: DataSourceProperties
) {

    @Bean
    @Primary
    fun dataSource(
        @Qualifier("readWriteDataSource") readWriteDataSource: DataSource,
        @Qualifier("readOnlyDataSource") readOnlyDataSource: DataSource
    ): TransactionRoutingDataSource = TransactionRoutingDataSource(
        readWriteDataSource,
        readOnlyDataSource
    )

    @Bean
    @Qualifier("readWriteDataSource")
    fun readWriteDataSource(): HikariDataSource = createDataSource(dataSourceProperties.url) as HikariDataSource

    @Bean
    @Qualifier("readOnlyDataSource")
    fun readOnlyDataSource(
        @Value("\${spring.datasource.readOnlyUrl}") readOnlyUrl: String
    ): HikariDataSource = createDataSource(readOnlyUrl) as HikariDataSource

    @Bean
    @Primary
    fun transactionManager(
        emf: EntityManagerFactory,
        dataSource: TransactionRoutingDataSource
    ): PlatformTransactionManager = ReplicaAwareTransactionManager(JpaTransactionManager(emf), dataSource)

    fun createDataSource(url: String): DataSource =
        DataSourceBuilder.create(dataSourceProperties.classLoader)
            .type(dataSourceProperties.type)
            .driverClassName(dataSourceProperties.determineDriverClassName())
            .username(dataSourceProperties.determineUsername())
            .password(dataSourceProperties.determinePassword())
            .url(url)
            .build()
}
