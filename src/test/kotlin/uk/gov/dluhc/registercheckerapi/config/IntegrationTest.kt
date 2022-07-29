package uk.gov.dluhc.registercheckerapi.config

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal abstract class IntegrationTest {

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun customizeWebTestClient() {
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(300))
            .build()
    }
}
