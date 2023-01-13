package uk.gov.dluhc.registercheckerapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckResultData
import java.util.UUID.randomUUID

internal class RegisterCheckResultDataRepositoryTest : IntegrationTest() {

    @Test
    fun `should save request body data`() {
        // Given
        val correlationId = randomUUID()
        val registerCheckResultData = buildRegisterCheckResultData(correlationId = correlationId)
        registerCheckResultDataRepository.save(registerCheckResultData)

        // When
        val actual = registerCheckResultDataRepository.findByCorrelationId(correlationId)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual?.id).isNotNull
        assertThat(actual?.correlationId).isEqualTo(correlationId)
        assertThat(actual?.requestBody?.contains(correlationId.toString())).isTrue
        assertThat(actual?.dateCreated).isNotNull
    }

    @Test
    fun `should find request body data within list of correlationIds`() {
        // Given
        val correlationId1 = randomUUID()
        val correlationId2 = randomUUID()
        val registerCheckResultData1 = buildRegisterCheckResultData(correlationId = correlationId1)
        val registerCheckResultData2 = buildRegisterCheckResultData(correlationId = correlationId2)
        val anotherRegisterCheckResultData = buildRegisterCheckResultData(correlationId = randomUUID())
        registerCheckResultDataRepository.saveAll(listOf(registerCheckResultData1, registerCheckResultData2, anotherRegisterCheckResultData))

        // When
        val actual = registerCheckResultDataRepository.findByCorrelationIdIn(listOf(correlationId1, correlationId2))

        // Then
        assertThat(actual).isNotNull.hasSize(2)
    }

    @Test
    fun `should return empty list for non-existing correlationIds register check result`() {
        // Given
        val correlationId1 = randomUUID()
        val correlationId2 = randomUUID()

        // When
        val actual = registerCheckResultDataRepository.findByCorrelationIdIn(listOf(correlationId1, correlationId2))

        // Then
        assertThat(actual).isNotNull.isEmpty()
    }
}
