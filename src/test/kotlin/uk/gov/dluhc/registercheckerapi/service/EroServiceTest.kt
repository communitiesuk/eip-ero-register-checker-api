package uk.gov.dluhc.registercheckerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeGeneralException
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId

@ExtendWith(MockitoExtension::class)
internal class EroServiceTest {

    @Mock
    private lateinit var electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient

    @InjectMocks
    private lateinit var eroService: EroService

    @Test
    fun `should lookup and return gssCodes`() {
        // Given
        val eroId = getRandomEroId()

        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any())).willReturn(
            ElectoralRegistrationOfficeResponse(
                eroId,
                "Test ERO",
                listOf(
                    LocalAuthorityResponse(
                        gssCode = "E123456789",
                        name = "Local Authority 1"
                    ),
                    LocalAuthorityResponse(
                        gssCode = "E987654321",
                        name = "Local Authority 2"
                    ),
                )
            )
        )

        val expectedGssCodes = listOf("E123456789", "E987654321")

        // When
        val gssCodes = eroService.lookupGssCodesForEro(eroId)

        // Then
        assertThat(gssCodes).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedGssCodes)
        verify(electoralRegistrationOfficeManagementApiClient).getElectoralRegistrationOffice(eroId)
    }

    @Test
    fun `should not return gssCodes given API client throws ERO not found exception`() {
        // Given
        val eroId = getRandomEroId()

        val expected = ElectoralRegistrationOfficeNotFoundException(eroId)
        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any())).willThrow(expected)

        // When
        val ex = catchThrowableOfType({ eroService.lookupGssCodesForEro(eroId) }, ElectoralRegistrationOfficeNotFoundException::class.java)

        // Then
        assertThat(ex).isEqualTo(expected)
    }

    @Test
    fun `should not return gssCodes given API client throws general exception`() {
        // Given
        val eroId = getRandomEroId()

        val expected = ElectoralRegistrationOfficeGeneralException("Some error getting ERO $eroId")
        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any())).willThrow(expected)

        // When
        val ex = catchThrowableOfType({ eroService.lookupGssCodesForEro(eroId) }, ElectoralRegistrationOfficeGeneralException::class.java)

        // Then
        assertThat(ex).isEqualTo(expected)
    }
}
