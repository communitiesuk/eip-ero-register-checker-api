package uk.gov.dluhc.registercheckerapi.rest

import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.config.database.TransactionRoutingDataSource
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.rest.GetPendingRegisterChecksIntegrationTest.Companion.CERT_SERIAL_NUMBER_VALUE
import uk.gov.dluhc.registercheckerapi.rest.GetPendingRegisterChecksIntegrationTest.Companion.REQUEST_HEADER_NAME
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultRequest
import java.util.UUID

internal class TransactionRoutingDataSourceIntegrationTest : IntegrationTest() {

    @Test
    fun `should use read-only data source for get function`() {
        // Given
        val eroIdFromIerApi = "camden-city-council"
        val gssCodeFromEroApi = getRandomGssCode()

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, gssCodeFromEroApi)

        // When
        webTestClient.get()
            .uri("/registerchecks")
            .header(
                REQUEST_HEADER_NAME,
                CERT_SERIAL_NUMBER_VALUE
            )
            .exchange()
            .expectStatus().isOk
            .returnResult(PendingRegisterChecksResponse::class.java)

        // Then
        then(readOnlyDataSource).should(atLeastOnce()).connection
    }

    @Test
    fun `should use read-write data source for updating function`() {
        // Given
        val requestId = UUID.fromString("322ff65f-a0a1-497d-a224-04800711a1fb")
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)

        // When
        webTestClient.post()
            .uri("/registerchecks/$requestId")
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        then(readOnlyDataSource).should(never()).connection
        then(readWriteDataSource).should(atLeastOnce()).connection
    }

    @Test
    fun `should have exactly three data sources`() {
        // The TransactionRoutingDataSource and the reading/writing pools.
        // This is important, because a slight change to config can cause AutoConfiguration
        // to pollute the context with stray DataSources.
        assertThat(dataSources).hasSize(3)
        assertThat(dataSources).hasExactlyElementsOfTypes(
            TransactionRoutingDataSource::class.java,
            HikariDataSource::class.java,
            HikariDataSource::class.java
        )
    }
}
