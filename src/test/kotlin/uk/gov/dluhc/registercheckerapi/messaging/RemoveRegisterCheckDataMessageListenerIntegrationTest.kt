package uk.gov.dluhc.registercheckerapi.messaging

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType
import uk.gov.dluhc.registercheckerapi.testsupport.MessagingTestHelper
import uk.gov.dluhc.registercheckerapi.testsupport.TestLogAppender
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheckResultData
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRemoveRegisterCheckDataMessage
import java.util.UUID.fromString
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType as SourceTypeModel

internal class RemoveRegisterCheckDataMessageListenerIntegrationTest : IntegrationTest() {

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
        val sourceReference = "93b62b87-0fa4-4d4a-89bf-4486f03f1000"
        val gssCode = "E09000567"

        val correlationIdForCheck1 = fromString("93b62b87-0fa4-4d4a-89bf-4486f03f1111")
        val correlationIdForCheck2 = fromString("93b62b87-0fa4-4d4a-89bf-4486f03f1222")
        val correlationIdForOtherSourceRef = fromString("33b62b87-0fa4-4d4a-89bf-4486f03f1333")
        val correlationIdForOtherGssCode = fromString("43b62b87-0fa4-4d4a-89bf-4486f03f1444")

        val registerCheckRecord1 = buildRegisterCheck(sourceReference = sourceReference, gssCode = gssCode, correlationId = correlationIdForCheck1, sourceType = expectedSourceType)
        val registerCheckRecord2 = buildRegisterCheck(sourceReference = sourceReference, gssCode = gssCode, correlationId = correlationIdForCheck2, sourceType = expectedSourceType)
        val registerCheckWithOtherSourceRef = buildRegisterCheck(sourceReference = randomUUID().toString(), gssCode = gssCode, correlationId = correlationIdForOtherSourceRef, sourceType = expectedSourceType)
        val registerCheckWithOtherGssCode = buildRegisterCheck(sourceReference = sourceReference, gssCode = getRandomGssCode(), correlationId = correlationIdForOtherGssCode, sourceType = expectedSourceType)
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
            sourceType = sourceTypeMessage,
            sourceReference = sourceReference,
        )

        // When
        sqsMessagingTemplate.send(removeApplicantRegisterCheckDataQueueName, message)

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(registerCheckRepository.findBySourceReferenceAndSourceType(sourceReference, sourceType = expectedSourceType)).isEmpty()
            assertThat(registerCheckResultDataRepository.findByCorrelationIdIn(setOf(correlationIdForCheck1, correlationIdForCheck2, correlationIdForOtherGssCode))).isEmpty()

            assertThat(registerCheckRepository.findByCorrelationId(correlationIdForOtherSourceRef)).isNotNull
            assertThat(registerCheckResultDataRepository.findByCorrelationIdIn(setOf(correlationIdForOtherSourceRef))).isNotEmpty.hasSize(1)

            assertThat(
                TestLogAppender.hasLog(
                    "RemoveRegisterCheckDataMessage received with " +
                        "sourceType: [${message.sourceType}] and " +
                        "sourceReference: [${message.sourceReference}]",
                    Level.INFO
                )
            )
            messagingTestHelper?.assertMessagesEnqueued(localStackContainerSettings.mappedQueueUrlForwardRemoveRegisterCheckData, 1)
        }
    }
}
