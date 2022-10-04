package uk.gov.dluhc.registercheckerapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatch
import java.time.Instant
import java.util.UUID

internal class RegisterCheckRepositoryTest : IntegrationTest() {

    @Nested
    inner class FindByGssCodes {
        @Test
        fun `should retrieve pending register checks by multiple gss codes`() {
            // Given
            val registerCheck1 = buildRegisterCheck(gssCode = "E09000020")
            val registerCheck2 = buildRegisterCheck(gssCode = "E09000021")
            val registerCheck3 = buildRegisterCheck(gssCode = "E09000022")
            registerCheckRepository.saveAll(listOf(registerCheck1, registerCheck2, registerCheck3))

            // When
            val actual = registerCheckRepository.findPendingEntriesByGssCodes(listOf("E09000020", "E09000022"))

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).containsExactlyInAnyOrder(registerCheck1, registerCheck3)
            actual.forEach {
                assertThat(it.id).isNotNull
                assertThat(it.dateCreated).isNotNull
                assertThat(it.updatedAt).isNotNull
                assertThat(it.version).isNotNull
                assertThat(it.registerCheckMatches).isEmpty()
            }
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
            val actual = registerCheckRepository.findPendingEntriesByGssCodes(listOf("E09000020"), 2)

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
            val actual = registerCheckRepository.findPendingEntriesByGssCodes(listOf("UNKNOWN"))

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    inner class FindByCorrelationId {
        @Test
        fun `should get pending register check by correlation id`() {
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

    @Nested
    inner class RecordMatchResult {
        @Test
        fun `should record exact match`() {
            // Given
            val registerCheck = buildRegisterCheck()
            registerCheckRepository.save(registerCheck)
            registerCheck.recordExactMatch(Instant.now(), buildRegisterCheckMatch())
            registerCheckRepository.save(registerCheck)

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheck.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheck)
            assertThat(actual?.status).isEqualTo(CheckStatus.EXACT_MATCH)
        }

        @Test
        fun `should record no match`() {
            // Given
            val registerCheck = buildRegisterCheck()
            registerCheckRepository.save(registerCheck)
            registerCheck.recordNoMatch(Instant.now())
            registerCheckRepository.save(registerCheck)

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheck.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheck)
            assertThat(actual?.status).isEqualTo(CheckStatus.NO_MATCH)
        }

        @Test
        fun `should record multiple matches`() {
            // Given
            val registerCheck = buildRegisterCheck()
            registerCheckRepository.save(registerCheck)
            registerCheck.recordMultipleMatches(Instant.now(), 2, listOf(buildRegisterCheckMatch(), buildRegisterCheckMatch()))
            registerCheckRepository.save(registerCheck)

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheck.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheck)
            assertThat(actual?.status).isEqualTo(CheckStatus.MULTIPLE_MATCH)
            assertThat(actual?.matchCount).isEqualTo(2)
            assertThat(actual?.registerCheckMatches?.size).isEqualTo(2)
        }

        @Test
        fun `should record too many matches`() {
            // Given
            val registerCheck = buildRegisterCheck()
            registerCheckRepository.save(registerCheck)
            val matches = mutableListOf<RegisterCheckMatch>().apply { repeat(10) { add(buildRegisterCheckMatch()) } }
            registerCheck.recordTooManyMatches(Instant.now(), 10, matches)
            registerCheckRepository.save(registerCheck)

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheck.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheck)
            assertThat(actual?.status).isEqualTo(CheckStatus.TOO_MANY_MATCHES)
            assertThat(actual?.matchCount).isEqualTo(10)
            assertThat(actual?.registerCheckMatches?.size).isEqualTo(10)
        }
    }
}
