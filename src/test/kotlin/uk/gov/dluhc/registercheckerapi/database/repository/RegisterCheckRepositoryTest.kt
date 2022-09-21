package uk.gov.dluhc.registercheckerapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.util.UUID

internal class RegisterCheckRepositoryTest : IntegrationTest() {

    @Test
    fun `should retrieve pending register checks by gss code`() {
        // Given
        val registerCheck1 = buildRegisterCheck(gssCode = "E09000020")
        val registerCheck2 = buildRegisterCheck(gssCode = "E09000021")
        registerCheckRepository.saveAll(listOf(registerCheck1, registerCheck2))

        // When
        val actual = registerCheckRepository.findPendingEntriesByGssCode("E09000020")

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).containsExactly(registerCheck1)
        assertThat(actual[0].id).isNotNull
        assertThat(actual[0].dateCreated).isNotNull
        assertThat(actual[0].updatedAt).isNotNull
        assertThat(actual[0].version).isNotNull
    }

    @Test
    fun `should retrieve limited number of pending register checks by gss code`() {
        // Given
        val registerCheck1 = buildRegisterCheck(gssCode = "E09000020")
        registerCheckRepository.save(registerCheck1)
        // sleep to guarantee order by date_created asc
        Thread.sleep(1000)
        val registerCheck2 = buildRegisterCheck(gssCode = "E09000020")
        registerCheckRepository.save(registerCheck2)
        Thread.sleep(1000)
        val registerCheck3 = buildRegisterCheck(gssCode = "E09000020")
        registerCheckRepository.save(registerCheck3)

        // When
        val actual = registerCheckRepository.findPendingEntriesByGssCode("E09000020", 2)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).containsExactly(registerCheck1, registerCheck2)
    }

    @Test
    fun `should not retrieve pending register checks by unknown gss code`() {
        // Given
        val registerCheck1 = buildRegisterCheck(gssCode = "E09000020")
        val registerCheck2 = buildRegisterCheck(gssCode = "E09000021")
        registerCheckRepository.saveAll(listOf(registerCheck1, registerCheck2))

        // When
        val actual = registerCheckRepository.findPendingEntriesByGssCode("UNKNOWN")

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).isEmpty()
    }

    @Test
    fun `should get register check by correlation id`() {
        // Given
        val registerCheck1 = buildRegisterCheck(gssCode = "E09000020")
        val registerCheck2 = buildRegisterCheck(gssCode = "E09000021")
        registerCheckRepository.saveAll(listOf(registerCheck1, registerCheck2))

        // When
        val actual = registerCheckRepository.findByCorrelationId(registerCheck1.correlationId)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(registerCheck1)
    }

    @Test
    fun `should not get register check by unknown correlation id`() {
        // Given
        val registerCheck1 = buildRegisterCheck(gssCode = "E09000020")
        val registerCheck2 = buildRegisterCheck(gssCode = "E09000021")
        registerCheckRepository.saveAll(listOf(registerCheck1, registerCheck2))

        // When
        val actual = registerCheckRepository.findByCorrelationId(UUID.randomUUID())

        // Then
        assertThat(actual).isNull()
    }
}
