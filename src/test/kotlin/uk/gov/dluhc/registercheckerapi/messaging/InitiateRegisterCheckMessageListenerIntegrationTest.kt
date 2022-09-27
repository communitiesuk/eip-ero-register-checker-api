package uk.gov.dluhc.registercheckerapi.messaging

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.Address
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.entity.RegisterCheckAssert
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildInitiateRegisterCheckMessage
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

private val logger = KotlinLogging.logger {}

internal class InitiateRegisterCheckMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val message = buildInitiateRegisterCheckMessage()
        val payload = objectMapper.writeValueAsString(message)
        val earliestDateCreated = Instant.now()
        val expected = RegisterCheck(
            correlationId = UUID.randomUUID(),
            sourceType = SourceType.VOTER_CARD,
            sourceReference = message.sourceReference,
            sourceCorrelationId = message.sourceCorrelationId,
            createdBy = message.requestedBy,
            gssCode = message.gssCode,
            status = CheckStatus.PENDING,
            version = 0L,
            personalDetail = PersonalDetail(
                firstName = message.personalDetail.firstName,
                middleNames = message.personalDetail.middleNames,
                surname = message.personalDetail.surname,
                dateOfBirth = message.personalDetail.dateOfBirth,
                phoneNumber = message.personalDetail.phone,
                email = message.personalDetail.email,
                address = Address(
                    property = message.personalDetail.address.property,
                    street = message.personalDetail.address.street,
                    locality = message.personalDetail.address.locality,
                    town = message.personalDetail.address.town,
                    area = message.personalDetail.address.area,
                    postcode = message.personalDetail.address.postcode,
                    uprn = message.personalDetail.address.uprn,
                )
            )
        )

        // When
        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(initiateApplicantRegisterCheckQueueName)
                .messageBody(payload)
                .build()
        )

        // Then
        val stopWatch = StopWatch.createStarted()
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val actualRegisterCheckJpaEntity = getActualRegisterCheckJpaEntity(message)
            logger.info("found actualRegisterCheckJpaEntitys[${actualRegisterCheckJpaEntity.size}]")
            Assertions.assertThat(actualRegisterCheckJpaEntity).hasSize(1)

            RegisterCheckAssert.assertThat(actualRegisterCheckJpaEntity.first())
                .ignoringIdFields()
                .ignoringDateFields()
                .isRecursivelyEqual(expected)
                .hasIdAndDbAuditFieldsAfter(earliestDateCreated)

            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }
    }

    private fun getActualRegisterCheckJpaEntity(message: InitiateRegisterCheckMessage): List<RegisterCheck> =
        registerCheckRepository.findAll { root: Root<RegisterCheck>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            cb.equal(root.get<UUID>("sourceCorrelationId"), message.sourceCorrelationId)
        }
}
