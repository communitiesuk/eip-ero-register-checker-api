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
    fun `should return OK with eroId given valid header key is present`() {
        // Given
        val certSerialNumberValue = "543219999"
        wireMockService.stubIerApiGetEroIdentifier()

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
        assertThat(actual).isEqualTo("1234")
    }

    @Test
    fun `should return Not found error given ier service throws 404`() {
        // Given
        val certSerialNumberValue = "543219888"
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError()

        // When
        val response = webTestClient.get()
            .uri("/registercheck")
            .header("client-cert-serial", certSerialNumberValue)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("EroId for certificate serial not found")
    }

    @Test
    fun `should return Internal Server error given ier service throws 500`() {
        // Given
        val certSerialNumberValue = "543219252"
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

        // When
        val response = webTestClient.get()
            .uri("/registercheck")
            .header("client-cert-serial", certSerialNumberValue)
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("Error getting eroId for certificate serial")
    }
}
