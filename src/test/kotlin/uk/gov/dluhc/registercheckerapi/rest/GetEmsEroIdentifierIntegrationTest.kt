package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
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
        // Given
        val certSerialNumberValue = "132131312321312"

        // When
        val response = webTestClient.get()
            .uri("/registercheck")
            .header("client-cert-serial", certSerialNumberValue)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(certSerialNumberValue)
    }
}
