package uk.gov.dluhc.registercheckerapi.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildElectoralRegistrationOfficeResponse

internal class ElectoralRegistrationOfficeManagementApiClientTest {

    private val exchangeFunction: ExchangeFunction = mock()

    private val clientResponse: ClientResponse = mock()

    private val clientRequest = ArgumentCaptor.forClass(ClientRequest::class.java)

    private val webClient = WebClient.builder()
        .baseUrl("http://ero-management-api")
        .exchangeFunction(exchangeFunction)
        .build()

    private val apiClient = ElectoralRegistrationOfficeManagementApiClient(webClient)

    @BeforeEach
    fun setupWebClientRequestCapture() {
        given(exchangeFunction.exchange(clientRequest.capture())).willReturn(Mono.just(clientResponse))
    }

    @Test
    fun `should get Electoral Registration Office`() {
        // Given
        val eroId = getRandomEroId()

        val expectedEro = buildElectoralRegistrationOfficeResponse(eroId = eroId)
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
            Mono.just(expectedEro)
        )

        // When
        val ero = apiClient.getElectoralRegistrationOffice(eroId)

        // Then
        assertThat(ero).isEqualTo(expectedEro)
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
    }

    @Test
    fun `should not get Electoral Registration Office given API returns a 404 error`() {
        // Given
        val eroId = getRandomEroId()

        val http404Error = NOT_FOUND.toWebClientResponseException()
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
            Mono.error(http404Error)
        )

        val expectedException = ElectoralRegistrationOfficeNotFoundException(eroId)

        // When
        val ex = catchThrowableOfType(
            { apiClient.getElectoralRegistrationOffice(eroId) },
            ElectoralRegistrationOfficeNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
    }

    @Test
    fun `should not get Electoral Registration Office given API returns a 500 error`() {
        // Given
        val eroId = getRandomEroId()

        val http500Error = INTERNAL_SERVER_ERROR.toWebClientResponseException()
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
            Mono.error(http500Error)
        )

        val expectedException =
            ElectoralRegistrationOfficeGeneralException("Error 500 INTERNAL_SERVER_ERROR getting ERO $eroId")

        // When
        val ex = catchThrowableOfType(
            { apiClient.getElectoralRegistrationOffice(eroId) },
            ElectoralRegistrationOfficeGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
    }
}

private fun HttpStatus.toWebClientResponseException(): WebClientResponseException =
    WebClientResponseException.create(this.value(), this.name, HttpHeaders.EMPTY, "".toByteArray(), null)
