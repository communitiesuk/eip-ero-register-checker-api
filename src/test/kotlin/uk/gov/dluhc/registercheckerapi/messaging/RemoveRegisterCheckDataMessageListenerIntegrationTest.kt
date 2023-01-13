package uk.gov.dluhc.registercheckerapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRemoveRegisterCheckDataMessage
import java.util.UUID.fromString
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit

internal class RemoveRegisterCheckDataMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val sourceReference = "93b62b87-0fa4-4d4a-89bf-4486f03f1000"
        val gssCode = "E09000567"

        val correlationIdForCheck1 = fromString("93b62b87-0fa4-4d4a-89bf-4486f03f1111")
        val correlationIdForCheck2 = fromString("93b62b87-0fa4-4d4a-89bf-4486f03f1222")
        val correlationIdForOtherSourceRef = fromString("33b62b87-0fa4-4d4a-89bf-4486f03f1333")
        val correlationIdForOtherGssCode = fromString("43b62b87-0fa4-4d4a-89bf-4486f03f1444")

        val registerCheckRecord1 = buildRegisterCheck(sourceReference = sourceReference, gssCode = gssCode, correlationId = correlationIdForCheck1)
        val registerCheckRecord2 = buildRegisterCheck(sourceReference = sourceReference, gssCode = gssCode, correlationId = correlationIdForCheck2)
        val registerCheckWithOtherSourceRef = buildRegisterCheck(sourceReference = randomUUID().toString(), gssCode = gssCode, correlationId = correlationIdForOtherSourceRef)
        val registerCheckWithOtherGssCode = buildRegisterCheck(sourceReference = sourceReference, gssCode = getRandomGssCode(), correlationId = correlationIdForOtherGssCode)
        registerCheckRepository.saveAll(listOf(registerCheckRecord1, registerCheckRecord2, registerCheckWithOtherSourceRef, registerCheckWithOtherGssCode))

        val registerCheckResultData1a = buildRegisterCheckResultData(correlationId = registerCheckRecord1.correlationId)
        val registerCheckResultData1b = buildRegisterCheckResultData(correlationId = registerCheckRecord1.correlationId)
        val registerCheckResultData2 = buildRegisterCheckResultData(correlationId = registerCheckRecord2.correlationId)
        val registerCheckResultDataForOtherSourceRef = buildRegisterCheckResultData(correlationId = registerCheckWithOtherSourceRef.correlationId)
        val registerCheckResultDataForOtherGssCode = buildRegisterCheckResultData(correlationId = registerCheckWithOtherGssCode.correlationId)
        registerCheckResultDataRepository.saveAll(
            listOf(
                registerCheckResultData1a, registerCheckResultData1b, registerCheckResultData2,
                registerCheckResultDataForOtherSourceRef, registerCheckResultDataForOtherGssCode
            )
        )

        val message = buildRemoveRegisterCheckDataMessage(
            sourceType = VOTER_MINUS_CARD,
            sourceReference = sourceReference,
            gssCode = gssCode
        )

        // When
        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(removeApplicantRegisterCheckDataQueueName)
                .messageBody(objectMapper.writeValueAsString(message))
                .build()
        )

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(registerCheckRepository.findBySourceReferenceAndSourceTypeAndGssCode(sourceReference, VOTER_CARD, gssCode)).isEmpty()
            assertThat(registerCheckResultDataRepository.findByCorrelationIdIn(setOf(correlationIdForCheck1, correlationIdForCheck2))).isEmpty()

            assertThat(registerCheckRepository.findByCorrelationId(correlationIdForOtherSourceRef)).isNotNull
            assertThat(registerCheckRepository.findByCorrelationId(correlationIdForOtherGssCode)).isNotNull
            assertThat(registerCheckResultDataRepository.findByCorrelationIdIn(setOf(correlationIdForOtherSourceRef, correlationIdForOtherGssCode))).isNotEmpty.hasSize(2)
        }
    }
}
