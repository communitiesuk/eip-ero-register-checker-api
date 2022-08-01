package uk.gov.dluhc.registercheckerapi.rest

import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

internal class GetEmsEroIdentifierIntegrationTest : IntegrationTest() {

    @Test
    fun `should return forbidden given valid header key is not present`() {
        webTestClient.get()
            .uri("/registercheck")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return OK given valid header key is present`() {
        webTestClient.get()
            .uri("/registercheck")
            .header("client-cert-serial", "132131312321312")
            .exchange()
            .expectStatus()
            .isOk
    }
}
