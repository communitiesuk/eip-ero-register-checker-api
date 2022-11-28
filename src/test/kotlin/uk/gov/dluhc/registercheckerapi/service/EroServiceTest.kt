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
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeGeneralException
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.registercheckerapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildLocalAuthorityResponse

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
        val expectedEro = buildElectoralRegistrationOfficeResponse(
            eroId = eroId,
            localAuthorities = mutableListOf(
                buildLocalAuthorityResponse(gssCode = "E12345678"),
                buildLocalAuthorityResponse(gssCode = "E98765432"),
            )
        )
        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any()))
            .willReturn(expectedEro)

        val expectedGssCodes = listOf("E12345678", "E98765432")

        // When
        val gssCodes = eroService.lookupGssCodesForEro(eroId)

        // Then
        assertThat(gssCodes).containsExactlyInAnyOrderElementsOf(expectedGssCodes)
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
