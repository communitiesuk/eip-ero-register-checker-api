package uk.gov.dluhc.registercheckerapi.messaging

import ch.qos.logback.classic.Level
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.Address
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheck
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.testsupport.MessagingTestHelper
import uk.gov.dluhc.registercheckerapi.testsupport.TestLogAppender
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.entity.RegisterCheckAssert
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildInitiateRegisterCheckMessage
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType as SourceTypeModel

private val logger = KotlinLogging.logger {}

internal class InitiateRegisterCheckMessageListenerIntegrationTest : IntegrationTest() {

    private var messagingTestHelper: MessagingTestHelper? = null

    @BeforeEach
    fun setup() {
        messagingTestHelper = MessagingTestHelper(sqsAsyncClient)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_MINUS_CARD, VOTER_CARD",
            "POSTAL_MINUS_VOTE, POSTAL_VOTE",
            "PROXY_MINUS_VOTE, PROXY_VOTE",
            "OVERSEAS_MINUS_VOTE, OVERSEAS_VOTE",
            "APPLICATIONS_MINUS_API, APPLICATIONS_API",
        ]
    )
    fun `should process message received on queue for all services`(
        sourceTypeMessage: SourceTypeModel,
        expectedSourceType: SourceType
    ) {
        // Given
        val message = buildInitiateRegisterCheckMessage(sourceType = sourceTypeMessage)
        val earliestDateCreated = Instant.now()
        val expected = RegisterCheck(
            correlationId = UUID.randomUUID(),
            sourceType = expectedSourceType,
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
            ),
            emsElectorId = message.emsElectorId,
            historicalSearch = message.historicalSearch,
        )

        // When
        sqsMessagingTemplate.send(initiateApplicantRegisterCheckQueueName, message)

        // Then
        val stopWatch = StopWatch.createStarted()
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val actualRegisterCheckJpaEntity = getActualRegisterCheckJpaEntity(message)
            assertThat(actualRegisterCheckJpaEntity).hasSize(1)

            RegisterCheckAssert.assertThat(actualRegisterCheckJpaEntity.first())
                .ignoringIdFields()
                .ignoringDateFields()
                .isRecursivelyEqual(expected)
                .hasIdAndDbAuditFieldsAfter(earliestDateCreated)

            assertThat(
                TestLogAppender.hasLog(
                    "New InitiateRegisterCheckMessage received with " +
                        "sourceReference: ${message.sourceReference} and " +
                        "sourceCorrelationId: ${message.sourceCorrelationId}",
                    Level.INFO
                )
            ).isTrue()

            messagingTestHelper?.assertMessagesEnqueued(localStackContainerSettings.mappedQueueUrlForwardInitiateRegisterCheckData, 1)

            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }
    }

    private fun getActualRegisterCheckJpaEntity(message: InitiateRegisterCheckMessage): List<RegisterCheck> =
        registerCheckRepository.findAll { root: Root<RegisterCheck>, _: CriteriaQuery<*>?, cb: CriteriaBuilder ->
            cb.equal(root.get<UUID>("sourceCorrelationId"), message.sourceCorrelationId)
        }
}
