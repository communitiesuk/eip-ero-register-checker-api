package uk.gov.dluhc.registercheckerapi.client

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import uk.gov.dluhc.external.ier.models.ErosGet200Response
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerEroDetailsList

@ExtendWith(MockitoExtension::class)
internal class IerApiClientTest {

    @Mock
    private lateinit var ierRestTemplate: RestTemplate

    @InjectMocks
    private lateinit var ierApiClient: IerApiClient

    @Test
    fun `should get a response with a list of ERODetails`() {
        // Given
        val expectedEros = buildIerEroDetailsList()
        val expectedUrl = "/eros"

        given(ierRestTemplate.getForEntity(anyString(), eq(ErosGet200Response::class.java)))
            .willReturn(ResponseEntity.ok(ErosGet200Response(expectedEros)))

        // When
        val actualEros = ierApiClient.getEros()

        // Then
        assertThat(actualEros).isEqualTo(expectedEros)

        verify(ierRestTemplate).getForEntity(expectedUrl, ErosGet200Response::class.java)
    }

    @Test
    fun `should return general exception when IER returns forbidden error`() {
        // Given
        val expectedUrl = "/eros"
        val exceptionMessage = "Forbidden while communicating to IER"
        val expectedException =
            IerGeneralException(message = "Error getting EROs from IER API")

        given(ierRestTemplate.getForEntity(anyString(), eq(ErosGet200Response::class.java)))
            .willThrow(HttpClientErrorException(HttpStatus.FORBIDDEN, exceptionMessage))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEros() },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(ierRestTemplate).getForEntity(expectedUrl, ErosGet200Response::class.java)
    }

    @Test
    fun `should return general exception when IER returns internal server error`() {
        // Given
        val expectedUrl = "/eros"
        val expectedException =
            IerGeneralException(message = "Error getting EROs from IER API")

        given(ierRestTemplate.getForEntity(anyString(), eq(ErosGet200Response::class.java)))
            .willThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEros() },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(ierRestTemplate).getForEntity(expectedUrl, ErosGet200Response::class.java)
    }
}
