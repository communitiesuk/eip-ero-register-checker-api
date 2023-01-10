package uk.gov.dluhc.registercheckerapi.messaging

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRemoveRegisterCheckDataMessage
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

internal class RemoveRegisterCheckDataMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val message = buildRemoveRegisterCheckDataMessage()
        val payload = objectMapper.writeValueAsString(message)

        // When
        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(removeApplicantRegisterCheckDataQueueName)
                .messageBody(payload)
                .build()
        )

        // Then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val registerCheckJpaEntity = getActualRegisterCheckJpaEntity(message)
            Assertions.assertThat(registerCheckJpaEntity).isEmpty()
            // TODO more assertions in subsequent subtasks, as currently empty entity will be returned everytime
        }
    }

    private fun getActualRegisterCheckJpaEntity(message: RemoveRegisterCheckDataMessage): List<RegisterCheck> =
        registerCheckRepository.findAll { root: Root<RegisterCheck>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            cb.equal(root.get<UUID>("sourceReference"), message.sourceReference)
        }
}
