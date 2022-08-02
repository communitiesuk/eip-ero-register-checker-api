package uk.gov.dluhc.registercheckerapi.config

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.dluhc.registercheckerapi.service.IerService
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.WiremockService

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "PT5M")
internal abstract class IntegrationTest {

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var wireMockService: WiremockService

    @Autowired
    protected lateinit var ierService: IerService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun resetAll() {
        clearAllCaches()
        wireMockService.resetAllStubsAndMappings()
    }

    fun clearAllCaches() {
        cacheManager
            .cacheNames
            .mapNotNull { cacheManager.getCache(it) }
            .forEach { it.clear() }
    }
}
