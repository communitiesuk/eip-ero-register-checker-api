package uk.gov.dluhc.registercheckerapi.rest

import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import reactor.core.publisher.Mono
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.entity.RegisterCheckAssert
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckMatchEntityFromRegisterCheckMatchApi
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckMatchRequest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckPersonalDetailFromMatchModel
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultRequest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

private const val REQUEST_HEADER_NAME = "client-cert-serial"
private const val CERT_SERIAL_NUMBER_VALUE = "543212222"

internal class UpdatePendingRegisterCheckIntegrationTest : IntegrationTest() {

    @Test
    fun `should return forbidden given valid header key is not present`() {
        webTestClient.post()
            .uri(buildUri())
            .contentType(APPLICATION_JSON)
            .body(
                Mono.just(buildRegisterCheckResultRequest()),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isForbidden

        // Then
        wireMockService.verifyGetEroIdentifierNeverCalled()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
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
        wireMockService.verifyGetEroIdentifierNeverCalled()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
    }

    @Test
    fun `should return bad request given request with invalid field names that cannot deserialize into a RegisterCheckResultRequest`() {
        // Given
        val requestBody = """
            {
              "requestid": "5e881061-57fd-4dc1-935f-8401ebe5758f",
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
                  "regstreet": "14 Churcher Close",
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
            .hasMessage("Validation failed for object='registerCheckResultRequest'. Error count: 2")
            .hasValidationError("Error on field 'gssCode': rejected value [1234], must match \"^[a-zA-Z]\\d{8}\$\"")
            .hasValidationError("Error on field 'registerCheckMatches[0].email': rejected value [not an email address], must be a well-formed email address")
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()

        assertMessageNotSubmittedToSqs(savedPendingRegisterCheckEntity.sourceReference)
    }

    @Test
    fun `should return success and submit message given pending register check with multiple match found for a given requestId`() {
        // Given
        val requestId = UUID.randomUUID()
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)

        val savedPendingRegisterCheckEntity = registerCheckRepository.save(
            buildRegisterCheck(
                correlationId = requestId,
                gssCode = firstGssCodeFromEroApi,
                status = CheckStatus.PENDING
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
            sourceType = RegisterCheckSourceType.VOTER_CARD,
            sourceReference = savedPendingRegisterCheckEntity.sourceReference,
            sourceCorrelationId = savedPendingRegisterCheckEntity.sourceCorrelationId,
            registerCheckResult = RegisterCheckResult.MULTIPLE_MATCH,
            matches = matches.map { buildRegisterCheckPersonalDetailFromMatchModel(it) }
        )
        val requestBody = buildRegisterCheckResultRequest(
            requestId = requestId,
            gssCode = firstGssCodeFromEroApi,
            createdAt = matchResultSentAt,
            registerCheckMatchCount = matchCount,
            registerCheckMatches = matches
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
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()

        val actualRegisterResultData = registerCheckResultDataRepository.findByCorrelationId(requestId)
        assertThat(actualRegisterResultData).isNotNull
        assertThat(actualRegisterResultData?.id).isNotNull
        assertThat(actualRegisterResultData?.correlationId).isNotNull
        assertThat(actualRegisterResultData?.dateCreated).isNotNull
        val persistedRequest = objectMapper.readValue(actualRegisterResultData!!.requestBody, RegisterCheckResultRequest::class.java)
        assertThat(persistedRequest).usingRecursiveComparison()
            .ignoringFields("registerCheckMatches.applicationCreatedAt")
            .isEqualTo(requestBody)

        assertMessageSubmittedToSqs(expectedMessageContent)
    }

    private fun assertMessageSubmittedToSqs(expectedMessageContent: RegisterCheckResultMessage) {
        val stopWatch = StopWatch.createStarted()
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val sqsMessages: List<Message> = getLatestSqsMessages()
            assertThat(sqsMessages).anyMatch {
                assertRegisterCheckResultMessage(it, expectedMessageContent)
            }

            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }
    }

    private fun assertMessageNotSubmittedToSqs(sourceReferenceNotExpected: String) {
        try {
            await.atMost(5, TimeUnit.SECONDS).until {
                val sqsMessages: List<Message> = getLatestSqsMessages()
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

    private fun getLatestSqsMessages(): List<Message> {
        val receiveMessageRequest =
            ReceiveMessageRequest(localStackContainerSettings.mappedQueueUrlConfirmRegisterCheckResult)
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

    private fun buildUri(requestId: UUID = UUID.randomUUID()) =
        "/registerchecks/$requestId"
}
