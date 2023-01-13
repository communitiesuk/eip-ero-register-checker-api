package uk.gov.dluhc.registercheckerapi.messaging

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRemoveRegisterCheckDataMessage
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

internal class RemoveRegisterCheckDataMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val sourceReference = randomUUID().toString()
        val sourceType = SourceType.VOTER_CARD

        val registerCheck1 = buildRegisterCheck(sourceReference = sourceReference, sourceType = sourceType)
        val registerCheck2 = buildRegisterCheck(sourceReference = sourceReference, sourceType = sourceType)
        registerCheckRepository.saveAll(listOf(registerCheck1, registerCheck2))

        val correlationId1 = registerCheck1.correlationId
        val correlationId2 = registerCheck2.correlationId

        val registerCheckResultData1a = buildRegisterCheckResultData(correlationId = correlationId1)
        val registerCheckResultData1b = buildRegisterCheckResultData(correlationId = correlationId1)
        val registerCheckResultData2 = buildRegisterCheckResultData(correlationId = correlationId2)
        registerCheckResultDataRepository.saveAll(listOf(registerCheckResultData1a, registerCheckResultData1b, registerCheckResultData2))

        val message = buildRemoveRegisterCheckDataMessage(
            sourceType = VOTER_MINUS_CARD,
            sourceReference = sourceReference
        )

        // When
        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(removeApplicantRegisterCheckDataQueueName)
                .messageBody(objectMapper.writeValueAsString(message))
                .build()
        )

        // Then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            Assertions.assertThat(getActualRegisterCheckJpaEntity(message)).isEmpty()
            Assertions.assertThat(registerCheckResultDataRepository.findByCorrelationIdIn(setOf(correlationId1, correlationId2))).isEmpty()
        }
    }

    private fun getActualRegisterCheckJpaEntity(message: RemoveRegisterCheckDataMessage): List<RegisterCheck> =
        registerCheckRepository.findAll { root: Root<RegisterCheck>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            cb.equal(root.get<String>("sourceReference"), message.sourceReference)
        }
}
