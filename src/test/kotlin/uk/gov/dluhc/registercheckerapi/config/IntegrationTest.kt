package uk.gov.dluhc.registercheckerapi.config

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.SqsClient
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckRepository
import uk.gov.dluhc.registercheckerapi.database.repository.RegisterCheckResultDataRepository
import uk.gov.dluhc.registercheckerapi.database.repository.VotingArrangementRepository
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.testsupport.TestLogAppender
import uk.gov.dluhc.registercheckerapi.testsupport.WiremockService
import java.time.Duration
import javax.sql.DataSource

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureWebTestClient(timeout = "PT5M")
internal abstract class IntegrationTest {

    @Autowired
    protected lateinit var localStackContainerSettings: LocalStackContainerSettings

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var wireMockService: WiremockService

    @Autowired
    protected lateinit var registerCheckRepository: RegisterCheckRepository

    @Autowired
    protected lateinit var registerCheckResultDataRepository: RegisterCheckResultDataRepository

    @Autowired
    protected lateinit var votingArrangementRepository: VotingArrangementRepository

    @Autowired
    protected lateinit var sqsClient: SqsClient

    @Autowired
    protected lateinit var amazonSQSAsync: AmazonSQSAsync

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var cacheManager: CacheManager

    @Autowired
    protected lateinit var sourceTypeMapper: SourceTypeMapper

    @SpyBean(name = "readWriteDataSource")
    protected lateinit var readWriteDataSource: HikariDataSource

    @SpyBean(name = "readOnlyDataSource")
    protected lateinit var readOnlyDataSource: HikariDataSource

    @Autowired
    protected lateinit var dataSources: List<DataSource>

    @Value("\${sqs.initiate-applicant-register-check-queue-name}")
    protected lateinit var initiateApplicantRegisterCheckQueueName: String

    @Value("\${sqs.remove-applicant-register-check-data-queue-name}")
    protected lateinit var removeApplicantRegisterCheckDataQueueName: String

    @Value("\${caching.time-to-live}")
    protected lateinit var timeToLive: Duration

    companion object {
        val mysqlContainerConfiguration: MySQLContainerConfiguration = MySQLContainerConfiguration.getInstance()
    }

    @BeforeEach
    fun resetWireMock() {
        wireMockService.resetAllStubsAndMappings()
    }

    @BeforeEach
    fun clearDatabase() {
        registerCheckResultDataRepository.deleteAll()
        registerCheckRepository.deleteAll()
        votingArrangementRepository.deleteAll()
        cacheManager.getCache(IER_ELECTORAL_REGISTRATION_OFFICES_CACHE)?.clear()
    }

    @BeforeEach
    fun clearLogAppender() {
        TestLogAppender.reset()
    }
}
