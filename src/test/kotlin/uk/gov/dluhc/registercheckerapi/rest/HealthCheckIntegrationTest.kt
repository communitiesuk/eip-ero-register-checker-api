package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

internal class HealthCheckIntegrationTest : IntegrationTest() {

    @Test
    fun `should return health check status UP given microservice is running healthily`() {
        // Given
        val request = webTestClient.get().uri("/actuator/health")

        // When
        val response = request.exchange()

        // Then
        response.expectStatus().isOk
        val actual = response.returnResult<String>().responseBody.blockFirst()
        assertThat(actual).isEqualTo("""{"status":"UP"}""")
    }
}
