package uk.gov.dluhc.registercheckerapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatch
import java.time.Instant
import java.util.UUID.randomUUID

internal class RegisterCheckRepositoryTest : IntegrationTest() {

    companion object {
        private val ID_FIELDS = arrayOf(
            "id",
            "personalDetail.id",
            "personalDetail.address.id",
            "correlationId",
        )
        private val DATE_FIELDS = arrayOf(
            "dateCreated",
            "historicalSearchEarliestDate",
            "personalDetail.dateCreated",
            "personalDetail.address.dateCreated",
            "updatedAt",
            "applicationCreatedAt",
        )
    }

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
            val actual = registerCheckRepository.findByCorrelationId(randomUUID())

            // Then
            assertThat(actual).isNull()
        }
    }

    @Nested
    inner class RecordMatchResult {

        @ParameterizedTest
        @CsvSource(
            value = [
                "EXACT_MATCH",
                "PENDING_DETERMINATION",
                "EXPIRED",
                "NOT_STARTED"
            ]
        )
        fun `should record exact match`(checkStatus: CheckStatus) {
            // Given
            val registerCheckToSearch = buildRegisterCheck()
            val registerCheckAnother = buildRegisterCheck()
            registerCheckRepository.saveAll(listOf(registerCheckToSearch, registerCheckAnother))
            registerCheckToSearch.recordExactMatch(
                checkStatus,
                Instant.now(),
                buildRegisterCheckMatch(),
                null
            )
            registerCheckAnother.recordNoMatch(Instant.now())
            registerCheckRepository.saveAll(listOf(registerCheckToSearch, registerCheckAnother))

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheckToSearch.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheckToSearch)
            assertThat(actual?.status).isEqualTo(checkStatus)
            assertThat(actual?.matchCount).isOne
            assertThat(actual?.registerCheckMatches).hasSize(1)
            for (registerCheckMatch in actual?.registerCheckMatches!!) {
                assertThat(registerCheckMatch.personalDetail).isNotNull
                assertThat(registerCheckMatch.personalDetail.address).isNotNull
            }
            assertThat(actual.matchResultSentAt).isNotNull
            assertThat(actual.historicalSearchEarliestDate).isNull()
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
            assertThat(actual?.matchCount).isZero
            assertThat(actual?.registerCheckMatches).isEmpty()
            assertThat(actual?.matchResultSentAt).isNotNull
        }

        @ParameterizedTest
        @ValueSource(ints = [2, 3, 4, 5, 6, 7, 8, 9, 10])
        fun `should record multiple matches`(matchCount: Int) {
            // Given
            val registerCheck = buildRegisterCheck()
            val historicalSearchEarliestDate = Instant.now()
            registerCheckRepository.save(registerCheck)
            val matches = mutableListOf<RegisterCheckMatch>().apply { repeat(matchCount) { add(buildRegisterCheckMatch()) } }
            registerCheck.recordMultipleMatches(Instant.now(), matchCount, matches, historicalSearchEarliestDate)
            registerCheckRepository.save(registerCheck)

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheck.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheck)
            assertThat(actual?.status).isEqualTo(CheckStatus.MULTIPLE_MATCH)
            assertThat(actual?.matchCount).isEqualTo(matchCount)
            assertThat(actual?.registerCheckMatches).hasSize(matches.size)
            for (registerCheckMatch in actual?.registerCheckMatches!!) {
                assertThat(registerCheckMatch.personalDetail).isNotNull
                assertThat(registerCheckMatch.personalDetail.address).isNotNull
            }
            assertThat(actual.matchResultSentAt).isNotNull
            assertThat(actual.historicalSearchEarliestDate).isNotNull
        }

        @ParameterizedTest
        @ValueSource(ints = [11, 100, 102])
        fun `should record too many matches`(matchCount: Int) {
            // Given
            val registerCheck = buildRegisterCheck()
            val historicalSearchEarliestDate = Instant.now()
            registerCheckRepository.save(registerCheck)
            val matches = mutableListOf<RegisterCheckMatch>().apply { repeat(matchCount) { add(buildRegisterCheckMatch()) } }
            registerCheck.recordTooManyMatches(Instant.now(), matchCount, matches, historicalSearchEarliestDate)
            registerCheckRepository.save(registerCheck)

            // When
            val actual = registerCheckRepository.findByCorrelationId(registerCheck.correlationId)

            // Then
            assertThat(actual).isNotNull
            assertThat(actual).isEqualTo(registerCheck)
            assertThat(actual?.status).isEqualTo(CheckStatus.TOO_MANY_MATCHES)
            assertThat(actual?.matchCount).isEqualTo(matchCount)
            assertThat(actual?.registerCheckMatches).hasSize(matches.size)
            for (registerCheckMatch in actual?.registerCheckMatches!!) {
                assertThat(registerCheckMatch.personalDetail).isNotNull
                assertThat(registerCheckMatch.personalDetail.address).isNotNull
            }
            assertThat(actual.matchResultSentAt).isNotNull
            assertThat(actual.historicalSearchEarliestDate).isNotNull
        }
    }

    @Nested
    inner class FindBySourceReferenceAndSourceType {

        @Test
        fun `should get register check by sourceReference and sourceType`() {
            // Given
            val sourceReference = randomUUID().toString()
            val gssCode = getRandomGssCode()
            val matchingRegisterCheck1 = buildRegisterCheck(sourceReference = sourceReference, gssCode = gssCode)
            val matchingRegisterCheck2 = buildRegisterCheck(sourceReference = sourceReference, gssCode = gssCode)
            val matchingRegisterCheck3 = buildRegisterCheck(sourceReference = sourceReference, gssCode = getRandomGssCode())
            val unMatchedRegisterCheck1 = buildRegisterCheck(sourceReference = randomUUID().toString())
            val unMatchedRegisterCheck2 = buildRegisterCheck()
            val expectedMatchingResults = 3

            registerCheckRepository.saveAll(
                listOf(
                    matchingRegisterCheck1,
                    matchingRegisterCheck2,
                    matchingRegisterCheck3,
                    unMatchedRegisterCheck1,
                    unMatchedRegisterCheck2
                )
            )

            // When
            val actual = registerCheckRepository.findBySourceReferenceAndSourceType(sourceReference, VOTER_CARD)

            // Then
            assertThat(actual)
                .isNotNull
                .hasSize(expectedMatchingResults)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringFields(*ID_FIELDS, *DATE_FIELDS)
                .isEqualTo(listOf(matchingRegisterCheck1, matchingRegisterCheck2, matchingRegisterCheck3))
        }

        @Test
        fun `should not get register check for an unknown sourceReference and sourceType`() {
            // Given
            val sourceReference = randomUUID().toString()
            val anotherSourceReference = randomUUID().toString()
            val unmatchedRegisterCheck1 = buildRegisterCheck(sourceReference = anotherSourceReference)
            val unmatchedRegisterCheck2 = buildRegisterCheck()
            registerCheckRepository.saveAll(listOf(unmatchedRegisterCheck1, unmatchedRegisterCheck2))

            // When
            val actual = registerCheckRepository.findBySourceReferenceAndSourceType(sourceReference, VOTER_CARD)

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }
    }
}
