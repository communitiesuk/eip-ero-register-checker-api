package uk.gov.dluhc.registercheckerapi.rest

import org.apache.commons.lang3.StringUtils.toRootUpperCase
import org.apache.commons.lang3.StringUtils.trim
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as SourceTypeEntity

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
        wireMockService.verifyIerGetErosNeverCalled()
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
        wireMockService.verifyIerGetErosNeverCalled()
        assertRequestIsAudited(requestIdInRequestBody)
    }

    @Test
    fun `should return not found error given IER service returns no ERO with a matching certificate serial number`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()
        val certificateSerialNumberFromIerApi = "543218888"

        wireMockService.stubIerApiGetEros(certificateSerialNumberFromIerApi, eroId, listOf(gssCode))

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
        wireMockService.verifyIerGetErosCalledOnce()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return not found given non-existing register check for a given requestId`() {
        // Given
        val requestId = UUID.fromString("322ff65f-a0a1-497d-a224-04800711a1fb")
        val eroId = "camden-city-council"
        val firstGssCode = "E12345678"
        val secondGssCode = "E98764532"
        val gssCodes = listOf(firstGssCode, secondGssCode)

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodes)

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
        wireMockService.verifyIerGetErosCalledOnce()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return bad request given registerCheckMatchCount in requestBody mismatches registerCheckMatches array size`() {
        // Given
        val eroId = "camden-city-council"
        val gssCode = "E12345678"
        val requestId = UUID.randomUUID()
        val registerCheckMatchCount = 10

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, listOf(gssCode))

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
        wireMockService.verifyIerGetErosNeverCalled()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return forbidden given gssCode in requestBody does not matches gssCode from ERO`() {
        // Given
        val eroId = "camden-city-council"
        val firstGssCodeFromIerApi = "E12345678"
        val secondGssCodeFromIerApi = "E98764532"
        val gssCodesFromIerApi = listOf(firstGssCodeFromIerApi, secondGssCodeFromIerApi)
        val gssCodeFromRequestBody = "E10101010"
        val requestId = UUID.randomUUID()

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodesFromIerApi)

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
        wireMockService.verifyIerGetErosCalledOnce()
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        val requestId = UUID.randomUUID()
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

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
            .hasMessage("Error retrieving EROs from IER API")
        wireMockService.verifyIerGetErosCalledOnce()
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
        val eroId = "camden-city-council"
        val firstGssCode = "E12345678"
        val secondGssCode = "E98764532"
        val gssCodes = listOf(firstGssCode, secondGssCode)
        val registerCheckMatchCountInRequest = 1 // Exact match

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodes)
        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = firstGssCode,
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
        wireMockService.verifyIerGetErosCalledOnce()

        assertRequestIsAudited(requestId)
        assertMessageNotSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            sourceReferenceNotExpected = savedPendingRegisterCheckEntity.sourceReference
        )
    }

    @Test
    fun `should return conflict given optimistic locking failure`() {
        // Given
        val requestId = UUID.fromString("14f66386-a86e-4dbc-af52-3327834f33d1")
        val eroId = "camden-city-council"
        val firstGssCode = "E12345678"
        val secondGssCode = "E98764532"
        val gssCodes = listOf(firstGssCode, secondGssCode)

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodes)

        registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = firstGssCode,
                status = CheckStatus.PENDING,
            )
        )

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        val responses = mutableListOf<FluxExchangeResult<ErrorResponse>>()

        // When
        // To force optimistic locking failure, two concurrent requests need to be run on separate threads
        val executor = Executors.newFixedThreadPool(2)
        for (i in 1..2) {
            executor.execute {
                responses.add(
                    webTestClient.post()
                        .uri(buildUri(requestId))
                        .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
                        .contentType(APPLICATION_JSON)
                        .body(
                            Mono.just(buildRegisterCheckResultRequest(requestId = requestId, registerCheckMatchCount = 1)),
                            RegisterCheckResultRequest::class.java
                        )
                        .exchange()
                        .returnResult(ErrorResponse::class.java)
                )
            }
        }
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        // One should be created and the other should error. This filters to just get the error and ignore the request that succeeded
        val notCreatedResponses = responses.filter { it.status != HttpStatus.CREATED }

        // Then
        assertThat(notCreatedResponses).hasSize(1)
        val actual = notCreatedResponses.first().responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(409)
            .hasError("Conflict")
            .hasMessage("Register check with requestid:[14f66386-a86e-4dbc-af52-3327834f33d1] has an optimistic locking failure")

        assertExactlyOneMessageSentToSqs(localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult)
    }

    @Test
    fun `should return created given a post request with multiple matches found`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroId = "camden-city-council"
        val gssCode = "E12345678"
        val anotherGssCode = "E98764532"
        val gssCodes = listOf(gssCode, anotherGssCode)
        val historicalSearchEarliestDate = OffsetDateTime.now(ZoneOffset.UTC)

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodes)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCode,
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
            gssCode = gssCode,
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

        wireMockService.verifyIerGetErosCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, matchResultSentAt.toString(), gssCode, matchCount)
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
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()
        val historicalSearchEarliestDate = OffsetDateTime.now(ZoneOffset.UTC)

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, listOf(gssCode))

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCode,
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
            gssCode = gssCode,
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

        wireMockService.verifyIerGetErosCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf((requestId)))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, createdAtFromRequest, gssCode, matchCount)

        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            expectedMessageContentSentToVca
        )
    }

    @Test
    fun `should return bad request given historic search earliest date before 1970`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()
        val createdAtFromRequest = "2022-09-13T21:03:03.7788394+05:30"
        val historicalSearchEarliestDate = OffsetDateTime.parse("1969-12-31T23:59:59Z")

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, listOf(gssCode))

        val bodyPayloadAsJson = buildJsonPayloadWithNoMatches(
            requestId = requestId.toString(),
            createdAt = createdAtFromRequest,
            gssCode = gssCode,
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.post()
            .uri(buildUri(requestId))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .contentType(APPLICATION_JSON)
            .bodyValue(bodyPayloadAsJson)
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
            .hasMessage("Request requestId:[$requestId] cannot have historicalSearchEarliestDate:[$historicalSearchEarliestDate] as dates before 1970 UTC are not valid")
        assertRequestIsAudited(requestId)
    }

    @Test
    fun `should return created given a historic search earliest date after 1970`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()
        val createdAtFromRequest = "2022-09-13T21:03:03.7788394+05:30"

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, listOf(gssCode))

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCode,
                status = CheckStatus.PENDING,
                historicalSearch = true,
                historicalSearchEarliestDate = null,
            )
        )

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
            historicalSearchEarliestDate = OffsetDateTime.parse("1970-01-01T00:00:01Z"),
            gssCode = gssCode
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

        wireMockService.verifyIerGetErosCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, createdAtFromRequest, gssCode, matchCount)

        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            expectedMessageContentSentToVca
        )
    }

    @Test
    fun `should return created given a post request with no matches found`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()
        val createdAtFromRequest = "2022-09-13T21:03:03.7788394+05:30"

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, listOf(gssCode))

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = gssCode,
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
            gssCode = gssCode
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

        wireMockService.verifyIerGetErosCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationIdIn(setOf(requestId))[0]
        assertRequestIsAudited(actualRegisterResultData, requestId, createdAtFromRequest, gssCode, matchCount)

        assertMessageSubmittedToSqs(
            queueUrl = localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult,
            expectedMessageContentSentToVca
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_CARD, VOTER_MINUS_CARD,",
            "POSTAL_VOTE, POSTAL_MINUS_VOTE,",
            "PROXY_VOTE, PROXY_MINUS_VOTE,",
            "OVERSEAS_VOTE, OVERSEAS_MINUS_VOTE,",
            "APPLICATIONS_API, APPLICATIONS_MINUS_API"
        ]
    )
    fun `should submit the different service result messages to the correct queues`(
        sourceTypeEntity: SourceTypeEntity,
        sourceType: SourceType
    ) {
        // Given
        val requestId = UUID.randomUUID()
        val eroId = "camden-city-council"
        val gssCode = "E12345678"
        val anotherGssCode = "E98764532"
        val gssCodes = listOf(gssCode, anotherGssCode)
        val historicalSearchEarliestDate = OffsetDateTime.now(ZoneOffset.UTC)

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodes)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                sourceType = sourceTypeEntity,
                correlationId = requestId,
                gssCode = gssCode,
                status = CheckStatus.PENDING,
                historicalSearchEarliestDate = null
            )
        )
        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID()))

        val matchResultSentAt = OffsetDateTime.now(ZoneOffset.UTC)
        val matchCount = 2
        val matches = listOf(buildRegisterCheckMatchRequest(), buildRegisterCheckMatchRequest())

        val expectedMessageContent = RegisterCheckResultMessage(
            sourceType = sourceType,
            sourceReference = savedPendingRegisterCheckEntity.sourceReference,
            sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
            registerCheckResult = RegisterCheckResult.MULTIPLE_MINUS_MATCH,
            matches = matches.map { buildVcaRegisterCheckMatchFromMatchApi(it) },
            historicalSearchEarliestDate = historicalSearchEarliestDate,
        )
        val requestBody = buildRegisterCheckResultRequest(
            requestId = requestId,
            gssCode = gssCode,
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
        val queueUrlForSourceType = getQueueUrlForSourceType(sourceType)
        assertMessageSubmittedToSqs(
            queueUrl = queueUrlForSourceType,
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

    private fun assertExactlyOneMessageSentToSqs(queueUrl: String) {
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val sqsMessages: List<Message> = getLatestSqsMessagesFromQueue(queueUrl)
            assertThat(sqsMessages.size).isEqualTo(1)
        }
    }

    private fun assertMessageNotSubmittedToSqs(queueUrl: String, sourceReferenceNotExpected: String) {
        try {
            await.atMost(5, TimeUnit.SECONDS).until {
                val sqsMessages: List<Message> = getLatestSqsMessagesFromQueue(queueUrl)
                assertThat(sqsMessages).noneMatch {
                    val actualRegisterCheckResultMessage = objectMapper.readValue(
                        it.body(),
                        RegisterCheckResultMessage::class.java,
                    )
                    actualRegisterCheckResultMessage.sourceReference == sourceReferenceNotExpected
                }
                false
            }
        } catch (expectedException: ConditionTimeoutException) {
            // expect timeout exception when successful
        }
    }

    private fun getLatestSqsMessagesFromQueue(queueUrl: String): List<Message> {
        val receiveMessageRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .build()

        return sqsAsyncClient.receiveMessage(receiveMessageRequest)
            .get()
            .messages()
    }

    private fun assertRegisterCheckResultMessage(
        actualMessage: Message,
        expectedMessage: RegisterCheckResultMessage
    ): Boolean {
        val actualRegisterCheckResultMessage = objectMapper.readValue(
            actualMessage.body(),
            RegisterCheckResultMessage::class.java,
        )

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
        historicalSearchEarliestDate: OffsetDateTime? = null
    ): String {
        return """
            {
            "requestid": "$requestId",
            "gssCode": "$gssCode",
            "createdAt": "$createdAt",
            ${if (historicalSearchEarliestDate == null) "" else "\"historicalSearchEarliestDate\": \"$historicalSearchEarliestDate\"," }
            "registerCheckMatchCount": 0
            }
        """.trimIndent()
    }

    private fun buildUri(requestId: UUID = UUID.randomUUID()) =
        "/registerchecks/$requestId"

    private fun getQueueUrlForSourceType(sourceType: SourceType): String {
        return when (sourceType) {
            SourceType.APPLICATIONS_MINUS_API -> localStackContainerSettings.mappedQueueUrlRegisterCheckResultResponse
            SourceType.VOTER_MINUS_CARD -> localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult
            SourceType.PROXY_MINUS_VOTE -> localStackContainerSettings.mappedQueueUrlProxyVoteConfirmRegisterCheckResult
            SourceType.POSTAL_MINUS_VOTE -> localStackContainerSettings.mappedQueueUrlPostalVoteConfirmRegisterCheckResult
            SourceType.OVERSEAS_MINUS_VOTE -> localStackContainerSettings.mappedQueueUrlOverseasVoteConfirmRegisterCheckResult
        }
    }
}
