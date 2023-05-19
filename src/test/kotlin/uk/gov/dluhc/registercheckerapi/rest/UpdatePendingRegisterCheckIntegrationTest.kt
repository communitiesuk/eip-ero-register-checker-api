package uk.gov.dluhc.registercheckerapi.rest

import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import org.apache.commons.lang3.StringUtils.toRootUpperCase
import org.apache.commons.lang3.StringUtils.trim
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.MediaType.APPLICATION_JSON
import reactor.core.publisher.Mono
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType.POSTAL_VOTE
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.entity.RegisterCheckAssert
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildAddress
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatchEntityFromRegisterCheckMatchApi
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildVcaRegisterCheckMatchFromMatchApi
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckMatchRequest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultRequest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val REQUEST_HEADER_NAME = "client-cert-serial"
const val CERT_SERIAL_NUMBER_VALUE = "543212222"

internal class UpdatePendingRegisterCheckIntegrationTest : IntegrationTest() {

    @Test
    fun `should return forbidden given valid header key is not present`() {
        // Given
        val requestId = UUID.randomUUID()

        // When
        webTestClient.post()
            .uri(buildUri(requestId))
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest()),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isForbidden

        // Then
        wireMockService.verifyIerGetEroIdentifierNeverCalled()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
        assertRequestIsNotAudited(requestId)
    }

    @Test
    fun `should return bad request given requestId in query param does not match with requestId in payload`() {
        // Given
        val requestIdInQueryParam = UUID.fromString("322ff65f-a0a1-497d-a224-04800711a1fb")
        val requestIdInRequestBody = UUID.fromString("533ff65f-a0a1-497d-a224-04800711a1fc")
        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestIdInQueryParam))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestIdInRequestBody)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessage("Request requestId:[322ff65f-a0a1-497d-a224-04800711a1fb] does not match with requestid:[533ff65f-a0a1-497d-a224-04800711a1fc] in body payload")
            .hasNoValidationErrors()
        wireMockService.verifyIerGetEroIdentifierNeverCalled()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
        assertRequestIsAudited(requestIdInRequestBody)
    }

    @Test
    fun `should return not found error given IER service throws 404`() {
        // Given
        val requestId = UUID.randomUUID()
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError(certificateSerial = CERT_SERIAL_NUMBER_VALUE)

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("EROCertificateMapping for certificateSerial=[543212222] not found")
        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return not found given non-existing register check for a given requestId`() {
        // Given
        val requestId = UUID.fromString("322ff65f-a0a1-497d-a224-04800711a1fb")
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("Pending register check for requestid:[322ff65f-a0a1-497d-a224-04800711a1fb] not found")
            .hasNoValidationErrors()
        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return bad request given registerCheckMatchCount in requestBody mismatches registerCheckMatches array size`() {
        // Given
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val requestId = UUID.randomUUID()
        val registerCheckMatchCount = 10

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi)

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(
                    buildRegisterCheckResultRequest(
                        requestId = requestId,
                        registerCheckMatchCount = registerCheckMatchCount,
                        registerCheckMatches = listOf(buildRegisterCheckMatchRequest())
                    )
                ),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessage("Request [registerCheckMatches:1] array size must be same as [registerCheckMatchCount:10] in body payload")
        wireMockService.verifyIerGetEroIdentifierNeverCalled()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return forbidden given gssCode in requestBody does not matches gssCode from ERO`() {
        // Given
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"
        val gssCodeFromRequestBody = "E10101010"
        val requestId = UUID.randomUUID()

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId, gssCode = gssCodeFromRequestBody)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isForbidden
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(403)
            .hasError("Forbidden")
            .hasMessage("Request gssCode:[E10101010] does not match with gssCode for certificateSerial:[543212222]")
        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        val requestId = UUID.randomUUID()
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError(certificateSerial = CERT_SERIAL_NUMBER_VALUE)

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(500)
            .hasError("Internal Server Error")
            .hasMessage("Error getting eroId for certificate serial")
        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return internal server error given ERO service throws 404`() {
        // Given
        val requestId = UUID.randomUUID()
        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, "camden-city-council")
        wireMockService.stubEroManagementGetEroThrowsNotFoundError()

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(500)
            .hasError("Internal Server Error")
            .hasMessage("Error retrieving GSS codes")
        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return bad request given request with invalid field names that cannot deserialize into a RegisterCheckResultRequest`() {
        // Given
        val requestIdInBody = UUID.fromString("5e881061-57fd-4dc1-935f-8401ebe5758f")
        val requestBody = """
            {
              "requestid": "$requestIdInBody",
              "gssCode": "T12345678",
              "createdAt": null,
              "registerCheckMatchCount": 1,
              "registerCheckMatches": [
                {
                  "emsElectorId": "70869",
                  "fn": "HAYWOOD",
                  "ln": "BECKETT",
                  "dob": "1980-07-31",
                  "regstreet": "14 Churcher Close",
                  "regtown": "Gosport",
                  "regarea": "Hampshire",
                  "regpostcode": "PO12 2SL",
                  "reguprn": "37026036",
                  "phone": "0777 1924066",
                  "email": "1924066@test.com",
                  "registeredStartDate": "2022-07-01",
                  "attestationCount": 0,
                  "franchiseCode": "G"
                }
              ]
            }            
        """.trimIndent() // request is invalid and cannot be deserialized (kotlin constructor) because createdAt is null

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessageContaining("Instantiation of [simple type, class uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest]")
            .hasMessageContaining("failed for JSON property createdAt due to missing (therefore NULL) value")
        assertRequestIsNotAudited(requestIdInBody)
    }

    @Test
    fun `should return bad request given request with invalid field values that fail constraint validation`() {
        // Given
        val requestBody = """
            {
              "requestid": "5e881061-57fd-4dc1-935f-8401ebe5758f",
              "gssCode": "1234",
              "createdAt": "2022-09-20T13:00:23.123Z",
              "registerCheckMatchCount": 1,
              "registerCheckMatches": [
                {
                  "emsElectorId": "70869",
                  "fn": "HAYWOOD",
                  "ln": "BECKETT",
                  "dob": "1980-07-31",
                  "regstreet": "",
                  "regtown": "Gosport",
                  "regarea": "Hampshire",
                  "regpostcode": "PO12 2SL",
                  "reguprn": "37026036",
                  "phone": "0777 1924066",
                  "email": "not an email address",
                  "registeredStartDate": "2022-07-01",
                  "attestationCount": 0,
                  "franchiseCode": "G"
                }
              ]
            }            
        """.trimIndent() // request has invalid gssCode and email field values

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessage("Validation failed for object='registerCheckResultRequest'. Error count: 1")
            .hasValidationError("Error on field 'gssCode': rejected value [1234], must match \"^[a-zA-Z]\\d{8}\$\"")
        assertRequestIsNotAudited(UUID.fromString("5e881061-57fd-4dc1-935f-8401ebe5758f"))
    }

    @Test
    fun `should return conflict given pending register check existing status is other than PENDING`() {
        // Given
        val requestId = UUID.fromString("14f66386-a86e-4dbc-af52-3327834f33d1")
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"
        val registerCheckMatchCountInRequest = 1 // Exact match

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)
        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = firstGssCodeFromEroApi,
                status = CheckStatus.NO_MATCH, // Existing record with NO_MATCH check status
                matchCount = 0
            )
        )

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest(requestId = requestId, registerCheckMatchCount = registerCheckMatchCountInRequest)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(409)
            .hasError("Conflict")
            .hasMessage("Register check with requestid:[14f66386-a86e-4dbc-af52-3327834f33d1] has an unexpected status:[NO_MATCH]")
            .hasNoValidationErrors()
        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()

        assertRequestIsAudited(requestId)
        assertMessageNotSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            sourceReferenceNotExpected = savedPendingRegisterCheckEntity.sourceReference
        )
    }

    @Test
    fun `should return created given a post request with multiple matches found`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroIdFromIerApi = "camden-city-council"
        val gssCodeFromEroApi = "E12345678"
        val anotherGssCodeFromEroApi = "E98764532"
        val historicalSearchEarliestDate = OffsetDateTime.now(ZoneOffset.UTC)

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, gssCodeFromEroApi, anotherGssCodeFromEroApi)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCodeFromEroApi,
                status = CheckStatus.PENDING,
                historicalSearchEarliestDate = null,
            )
        )
        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID()))

        val matchResultSentAt = OffsetDateTime.now(ZoneOffset.UTC)
        val matchCount = 2
        val matches = listOf(buildRegisterCheckMatchRequest(), buildRegisterCheckMatchRequest())
        val expectedRegisterCheckMatchEntityList = listOf(
            buildRegisterCheckMatchEntityFromRegisterCheckMatchApi(matches[0]),
            buildRegisterCheckMatchEntityFromRegisterCheckMatchApi(matches[1])
        )
        val expectedMessageContent = RegisterCheckResultMessage(
            sourceType = SourceType.VOTER_MINUS_CARD,
            sourceReference = savedPendingRegisterCheckEntity.sourceReference,
            sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
            registerCheckResult = RegisterCheckResult.MULTIPLE_MINUS_MATCH,
            matches = matches.map { buildVcaRegisterCheckMatchFromMatchApi(it) },
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )
        val requestBody = buildRegisterCheckResultRequest(
            requestId = requestId,
            gssCode = gssCodeFromEroApi,
            createdAt = matchResultSentAt,
            registerCheckMatchCount = matchCount,
            registerCheckMatches = matches,
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )

        // When
        webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(requestBody),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isCreated

        // Then
        val actualRegisterCheckJpaEntity = registerCheckRepository.findByCorrelationId(requestId)
        RegisterCheckAssert
            .assertThat(actualRegisterCheckJpaEntity)
            .ignoringIdFields()
            .ignoringDateFields()
            .hasStatus(CheckStatus.MULTIPLE_MATCH)
            .hasMatchResultSentAt(matchResultSentAt.toInstant())
            .hasMatchCount(matchCount)
            .hasRegisterCheckMatches(expectedRegisterCheckMatchEntityList)

        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, matchResultSentAt.toString(), gssCodeFromEroApi, matchCount)
        val persistedRequest = objectMapper.readValue(actualRegisterResultData.requestBody, RegisterCheckResultRequest::class.java)
        assertThat(persistedRequest).usingRecursiveComparison()
            .ignoringFields("registerCheckMatches.applicationCreatedAt")
            .isEqualTo(requestBody)

        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            expectedMessageContent
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            // handle date format with timezone offset and empty franchise code
            "2022-09-13T21:03:03.7788394+05:30, '', BS1 1AB, BS1 1AB, EXACT_MINUS_MATCH, EXACT_MATCH",
            // handle UTC date format and Pending franchise code
            "1986-05-01T02:42:44.348Z, Pending,  BS1 1AB, BS1 1AB, PENDING_MINUS_DETERMINATION, PENDING_DETERMINATION",
            // handle partial match
            "2022-11-17T21:33:03.7788394+00:00, '', BS1 1AB, BS2 2CD, PARTIAL_MINUS_MATCH, PARTIAL_MATCH"
        ]
    )
    fun `should return created given a post request with one match found`(
        createdAtFromRequest: String,
        franchiseCodeFromRequest: String,
        postcodeFromRequest: String,
        applicationPostcode: String,
        expectedRegisterCheckResult: RegisterCheckResult,
        expectedCheckStatus: CheckStatus,
    ) {
        // Given
        val requestId = UUID.randomUUID()
        val eroIdFromIerApi = "camden-city-council"
        val gssCodeFromEroApi = getRandomGssCode()
        val historicalSearchEarliestDate = OffsetDateTime.now(ZoneOffset.UTC)

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, gssCodeFromEroApi)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCodeFromEroApi,
                status = CheckStatus.PENDING,
                personalDetail = buildPersonalDetail(
                    address = buildAddress(postcode = applicationPostcode)
                ),
                historicalSearchEarliestDate = null
            )
        )
        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID()))

        val matchResultSentAt = OffsetDateTime.parse(createdAtFromRequest)
        val matchCount = 1
        val matches = listOf(
            buildRegisterCheckMatchRequest(
                franchiseCode = toRootUpperCase(trim(franchiseCodeFromRequest)),
                fn = savedPendingRegisterCheckEntity.personalDetail.firstName,
                ln = savedPendingRegisterCheckEntity.personalDetail.surname,
                dob = savedPendingRegisterCheckEntity.personalDetail.dateOfBirth,
                regproperty = savedPendingRegisterCheckEntity.personalDetail.address.property,
                regstreet = savedPendingRegisterCheckEntity.personalDetail.address.street,
                regpostcode = postcodeFromRequest
            )
        )
        val expectedRegisterCheckMatchEntityList = listOf(
            buildRegisterCheckMatchEntityFromRegisterCheckMatchApi(matches[0])
        )
        val expectedMessageContentSentToVca = buildRegisterCheckResultMessage(
            sourceType = SourceType.VOTER_MINUS_CARD,
            sourceReference = savedPendingRegisterCheckEntity.sourceReference,
            sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
            registerCheckResult = expectedRegisterCheckResult,
            matches = matches.map { buildVcaRegisterCheckMatchFromMatchApi(it) },
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )

        val requestBody = buildRegisterCheckResultRequest(
            requestId = requestId,
            gssCode = gssCodeFromEroApi,
            createdAt = matchResultSentAt,
            registerCheckMatchCount = matchCount,
            registerCheckMatches = matches,
            historicalSearchEarliestDate = historicalSearchEarliestDate
        )

        // When
        webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(requestBody),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isCreated

        // Then
        val actualRegisterCheckJpaEntity = registerCheckRepository.findByCorrelationId(requestId)
        RegisterCheckAssert
            .assertThat(actualRegisterCheckJpaEntity)
            .ignoringIdFields()
            .ignoringDateFields()
            .hasStatus(expectedCheckStatus)
            .hasMatchResultSentAt(matchResultSentAt.toInstant())
            .hasMatchCount(matchCount)
            .hasRegisterCheckMatches(expectedRegisterCheckMatchEntityList)

        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf((requestId)))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, createdAtFromRequest, gssCodeFromEroApi, matchCount)

        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            expectedMessageContentSentToVca
        )
    }

    @Test
    fun `should return created given a post request with no matches found`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroIdFromIerApi = "camden-city-council"
        val gssCodeFromEroApi = getRandomGssCode()
        val createdAtFromRequest = "2022-09-13T21:03:03.7788394+05:30"

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, gssCodeFromEroApi)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCodeFromEroApi,
                status = CheckStatus.PENDING,
                historicalSearchEarliestDate = null
            )
        )
        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID()))

        val expectedMessageContentSentToVca = RegisterCheckResultMessage(
            sourceType = SourceType.VOTER_MINUS_CARD,
            sourceReference = savedPendingRegisterCheckEntity.sourceReference,
            sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
            registerCheckResult = RegisterCheckResult.NO_MINUS_MATCH,
            matches = emptyList()
        )

        val matchCount = 0
        val bodyPayloadAsJson = buildJsonPayloadWithNoMatches(
            requestId = requestId.toString(),
            createdAt = createdAtFromRequest,
            gssCode = gssCodeFromEroApi
        )
        val matchResultSentAt = OffsetDateTime.parse(createdAtFromRequest)

        // When
        webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .bodyValue(bodyPayloadAsJson)
            .exchange()
            .expectStatus()
            .isCreated

        // Then
        val actualRegisterCheckJpaEntity = registerCheckRepository.findByCorrelationId(requestId)
        RegisterCheckAssert
            .assertThat(actualRegisterCheckJpaEntity)
            .ignoringIdFields()
            .ignoringDateFields()
            .hasStatus(CheckStatus.NO_MATCH)
            .hasMatchResultSentAt(matchResultSentAt.toInstant())
            .hasMatchCount(matchCount)

        wireMockService.verifyIerGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, createdAtFromRequest, gssCodeFromEroApi, matchCount)

        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            expectedMessageContentSentToVca
        )
    }

    @Test
    fun `should submit the POSTAL_VOTE result message to the postal vote queue`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroIdFromIerApi = "camden-city-council"
        val gssCodeFromEroApi = "E12345678"
        val anotherGssCodeFromEroApi = "E98764532"
        val historicalSearchEarliestDate = OffsetDateTime.now(ZoneOffset.UTC)

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, gssCodeFromEroApi, anotherGssCodeFromEroApi)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                sourceType = POSTAL_VOTE,
                correlationId = requestId,
                gssCode = gssCodeFromEroApi,
                status = CheckStatus.PENDING,
                historicalSearchEarliestDate = null
            )
        )
        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID()))

        val matchResultSentAt = OffsetDateTime.now(ZoneOffset.UTC)
        val matchCount = 2
        val matches = listOf(buildRegisterCheckMatchRequest(), buildRegisterCheckMatchRequest())

        val expectedMessageContent = RegisterCheckResultMessage(
            sourceType = sourceTypeMapper.fromEntityToVcaSqsEnum(POSTAL_VOTE),
            sourceReference = savedPendingRegisterCheckEntity.sourceReference,
            sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
            registerCheckResult = RegisterCheckResult.MULTIPLE_MINUS_MATCH,
            matches = matches.map { buildVcaRegisterCheckMatchFromMatchApi(it) },
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )
        val requestBody = buildRegisterCheckResultRequest(
            requestId = requestId,
            gssCode = gssCodeFromEroApi,
            createdAt = matchResultSentAt,
            registerCheckMatchCount = matchCount,
            registerCheckMatches = matches,
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )

        // When
        webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(requestBody),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isCreated

        // Then
        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlPostalVoteConfirmRegisterCheckResult,
            expectedMessageContent = expectedMessageContent
        )
    }

    private fun assertMessageSubmittedToSqs(queueUrl: String, expectedMessageContent: RegisterCheckResultMessage) {
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val sqsMessages: List<Message> = getLatestSqsMessagesFromQueue(queueUrl)
            assertThat(sqsMessages).anyMatch {
                assertRegisterCheckResultMessage(it, expectedMessageContent)
            }
        }
    }

    private fun assertMessageNotSubmittedToSqs(queueUrl: String, sourceReferenceNotExpected: String) {
        try {
            await.atMost(5, TimeUnit.SECONDS).until {
                val sqsMessages: List<Message> = getLatestSqsMessagesFromQueue(queueUrl)
                assertThat(sqsMessages).noneMatch {
                    val actualRegisterCheckResultMessage = objectMapper.readValue(it.body, RegisterCheckResultMessage::class.java)
                    actualRegisterCheckResultMessage.sourceReference == sourceReferenceNotExpected
                }
                false
            }
        } catch (expectedException: ConditionTimeoutException) {
            // expect timeout exception when successful
        }
    }

    private fun getLatestSqsMessagesFromQueue(queueUrl: String): List<Message> {
        val receiveMessageRequest =
            ReceiveMessageRequest(queueUrl)
                .withMaxNumberOfMessages(10)

        return amazonSQSAsync.receiveMessage(receiveMessageRequest).messages
    }

    private fun assertRegisterCheckResultMessage(
        actualMessage: Message,
        expectedMessage: RegisterCheckResultMessage
    ): Boolean {
        val actualRegisterCheckResultMessage = objectMapper.readValue(actualMessage.body, RegisterCheckResultMessage::class.java)

        assertThat(actualRegisterCheckResultMessage)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedMessage)
        return true
    }

    private fun assertRequestIsNotAudited(requestId: UUID) {
        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))
        assertThat(actualRegisterResultData).isEmpty()
    }

    private fun assertRequestIsAudited(requestId: UUID) {
        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))[0]
        assertThat(actualRegisterResultData).isNotNull
        assertThat(actualRegisterResultData.id).isNotNull
        assertThat(actualRegisterResultData.correlationId).isNotNull
        assertThat(actualRegisterResultData.dateCreated).isNotNull
    }

    private fun assertRequestIsAudited(
        actualRegisterResultData: RegisterCheckResultData?,
        expectedRequestId: UUID,
        expectedCreatedAt: String,
        expectedGssCode: String,
        matchCount: Int
    ) {
        assertThat(actualRegisterResultData).isNotNull
        assertThat(actualRegisterResultData!!.id).isNotNull
        assertThat(actualRegisterResultData.correlationId).isNotNull
        assertThat(actualRegisterResultData.dateCreated).isNotNull
        val persistedRequest = objectMapper.readValue(actualRegisterResultData.requestBody, RegisterCheckResultRequest::class.java)
        assertThat(persistedRequest.requestid).isEqualTo(expectedRequestId)
        assertThat(persistedRequest.createdAt).isEqualTo(expectedCreatedAt)
        assertThat(persistedRequest.gssCode).isEqualTo(expectedGssCode)
        assertThat(persistedRequest.registerCheckMatchCount).isEqualTo(matchCount)
    }

    private fun buildJsonPayloadWithNoMatches(
        requestId: String,
        createdAt: String,
        gssCode: String,
    ): String {
        return """
            {
            "requestid": "$requestId",
            "gssCode": "$gssCode",
            "createdAt": "$createdAt",
            "registerCheckMatchCount": 0
            }
        """.trimIndent()
    }

    private fun buildUri(requestId: UUID = UUID.randomUUID()) =
        "/registerchecks/$requestId"
}
