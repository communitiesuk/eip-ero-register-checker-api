package uk.gov.dluhc.registercheckerapi.client

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.dluhc.registercheckerapi.models.EROCertificateMapping

internal class IerManagementApiClientTest {

    private val exchangeFunction: ExchangeFunction = mock()

    private val clientResponse: ClientResponse = mock()

    private val clientRequest = ArgumentCaptor.forClass(ClientRequest::class.java)

    private val webClient = WebClient.builder()
        .baseUrl("http://ier-management-api")
        .exchangeFunction(exchangeFunction)
        .build()

    private val apiClient = IerManagementApiClient(webClient)

    @BeforeEach
    fun setupWebClientRequestCapture() {
        given(exchangeFunction.exchange(clientRequest.capture()))
            .willReturn(Mono.just(clientResponse))
    }

    @Test
    fun `should get EROCertificateMapping for a given certificate serial`() {
        // Given
        val certificateSerial = "123456789"
        val expectedEroCertificateMapping = EROCertificateMapping("1234", certificateSerial)

        given(clientResponse.bodyToMono(EROCertificateMapping::class.java)).willReturn(
            Mono.just(expectedEroCertificateMapping)
        )

        // When
        val actualEroCertificateMapping = apiClient.getEroIdentifier(certificateSerial)

        // Then
        assertThat(actualEroCertificateMapping).isEqualTo(expectedEroCertificateMapping)
        assertThat(clientRequest.value.url())
            .hasHost("ier-management-api")
            .hasPath("/ier-ero/eros")
            .hasParameter("certificateSerial", certificateSerial)
    }

    @Test
    fun `should not get EROCertificateMapping response given API returns a 404 error`() {
        // Given
        val certificateSerial = "123456789"

        val http404Error = HttpStatus.NOT_FOUND.toWebClientResponseException()
        given(clientResponse.bodyToMono(EROCertificateMapping::class.java)).willReturn(
            Mono.error(http404Error)
        )

        val expectedException = IerNotFoundException(certificateSerial)

        // When
        val ex = Assertions.catchThrowableOfType(
            { apiClient.getEroIdentifier(certificateSerial) },
            IerNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertThat(clientRequest.value.url())
            .hasHost("ier-management-api")
            .hasPath("/ier-ero/eros")
            .hasParameter("certificateSerial", certificateSerial)
    }

    @Test
    fun `should not get EROCertificateMapping given API returns a 500 error`() {
        // Given
        val certificateSerial = "123456789"

        val http500Error = HttpStatus.INTERNAL_SERVER_ERROR.toWebClientResponseException()
        given(clientResponse.bodyToMono(EROCertificateMapping::class.java)).willReturn(Mono.error(http500Error))

        val expectedException =
            IerGeneralException("Error 500 INTERNAL_SERVER_ERROR getting EROCertificateMapping for certificate serial 123456789")

        // When
        val ex = Assertions.catchThrowableOfType(
            { apiClient.getEroIdentifier(certificateSerial) },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertThat(clientRequest.value.url())
            .hasHost("ier-management-api")
            .hasPath("/ier-ero/eros")
            .hasParameter("certificateSerial", certificateSerial)
    }

    private fun HttpStatus.toWebClientResponseException(): WebClientResponseException =
        WebClientResponseException.create(this.value(), this.name, HttpHeaders.EMPTY, "".toByteArray(), null)
}
