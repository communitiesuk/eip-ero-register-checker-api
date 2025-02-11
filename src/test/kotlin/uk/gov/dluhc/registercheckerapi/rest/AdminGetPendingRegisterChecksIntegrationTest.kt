package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.models.AdminPendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck

internal class AdminGetPendingRegisterChecksIntegrationTest : IntegrationTest() {

    companion object {
        private const val ADMIN_GET_PENDING_REGISTER_CHECKS_ENDPOINT = "/admin/pending-checks/"
    }

    @Test
    fun `should return ok with empty pending register checks`() {
        // Given
        val eroId = "south-testington"
        val gssCode = getRandomGssCode()

        wireMockService.stubIerApiGetEros("", eroId, listOf(gssCode))

        // When
        val response = webTestClient.get()
            .uri(buildUri(eroId))
            .exchange()
            .expectStatus().isOk
            .returnResult(AdminPendingRegisterChecksResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.pendingRegisterChecks).isEmpty()
        wireMockService.verifyIerGetErosCalledOnce()
    }

    @Test
    fun `should return ok with multiple pending register checks`() {
        // Given
        val eroId = "north-testington"
        val firstGssCode = getRandomGssCode()
        val secondGssCode = getRandomGssCode()
        val gssCodes = listOf(firstGssCode, secondGssCode)
        val expectedRecordCount = 3

        wireMockService.stubIerApiGetEros("", eroId, gssCodes)

        val pendingCheck1 = registerCheckRepository.save(buildRegisterCheck(gssCode = firstGssCode, status = CheckStatus.PENDING))
        Thread.sleep(1000)
        val pendingCheck2 = registerCheckRepository.save(buildRegisterCheck(gssCode = firstGssCode, status = CheckStatus.PENDING))
        Thread.sleep(1000)
        val pendingCheck3 = registerCheckRepository.save(buildRegisterCheck(gssCode = secondGssCode, status = CheckStatus.PENDING))

        // When
        val response = webTestClient.get()
            .uri(buildUri(eroId))
            .exchange()
            .expectStatus().isOk
            .returnResult(AdminPendingRegisterChecksResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.pendingRegisterChecks).hasSize(expectedRecordCount)
        assertThat(actual.pendingRegisterChecks.map { it.applicationId }).isEqualTo(
            listOf(pendingCheck1.sourceReference, pendingCheck2.sourceReference, pendingCheck3.sourceReference)
        )
    }

    @Test
    fun `should return not found error given IER service returns no matching ERO`() {
        val eroId = "east-testington"
        val gssCode = getRandomGssCode()

        wireMockService.stubIerApiGetEros("", "west-testington", listOf(gssCode))

        // When
        val response = webTestClient.get()
            .uri(buildUri(eroId))
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("ERO with eroId=[$eroId] not found")
    }

    @Test
    fun `should return internal server error given IER service fails`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

        // When
        val response = webTestClient.get()
            .uri(buildUri("west-testington"))
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(500)
            .hasError("Internal Server Error")
            .hasMessage("Error retrieving EROs from IER API")
    }

    private fun buildUri(eroId: String) =
        UriComponentsBuilder
            .fromUriString(ADMIN_GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .path(eroId)
            .build().toUriString()
}
