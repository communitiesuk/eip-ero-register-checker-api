package uk.gov.dluhc.registercheckerapi.client

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import uk.gov.dluhc.external.ier.models.ErosGet200Response
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildIerEroDetailsList

@ExtendWith(MockitoExtension::class)
internal class IerApiClientTest {

    private val getRequest: RestClient.RequestHeadersUriSpec<*> = mock()

    private val uriSpec: RestClient.RequestHeadersUriSpec<*> = mock()

    @Mock
    private lateinit var ierRestClient: RestClient

    @InjectMocks
    private lateinit var ierApiClient: IerApiClient

    @BeforeEach
    fun setup() {
        given(ierRestClient.get()).willReturn(getRequest)
        given(getRequest.uri(anyString())).willReturn(uriSpec)
    }

    @Test
    fun `should get a response with a list of ERODetails`() {
        // Given
        val expectedEros = buildIerEroDetailsList()
        val expectedUrl = "/eros"

        val mockResponseSpec: RestClient.ResponseSpec = mock()
        given(uriSpec.retrieve()).willReturn(mockResponseSpec)
        given(mockResponseSpec.toEntity(any<Class<ErosGet200Response>>()))
            .willReturn(ResponseEntity.ok(ErosGet200Response(expectedEros)))

        // When
        val actualEros = ierApiClient.getEros()

        // Then
        assertThat(actualEros).isEqualTo(expectedEros)

        verify(getRequest).uri(expectedUrl)
    }

    @Test
    fun `should return general exception when IER returns forbidden error`() {
        // Given
        val expectedUrl = "/eros"
        val exceptionMessage = "Forbidden while communicating to IER"
        val expectedException =
            IerGeneralException(message = "Error getting EROs from IER API")

        given(uriSpec.retrieve())
            .willThrow(HttpClientErrorException(HttpStatus.FORBIDDEN, exceptionMessage))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEros() },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(getRequest).uri(expectedUrl)
    }

    @Test
    fun `should return general exception when IER returns internal server error`() {
        // Given
        val expectedUrl = "/eros"
        val expectedException =
            IerGeneralException(message = "Error getting EROs from IER API")

        given(uriSpec.retrieve())
            .willThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))

        // When
        val ex = Assertions.catchThrowableOfType(
            { ierApiClient.getEros() },
            IerGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        verify(getRequest).uri(expectedUrl)
    }
}
