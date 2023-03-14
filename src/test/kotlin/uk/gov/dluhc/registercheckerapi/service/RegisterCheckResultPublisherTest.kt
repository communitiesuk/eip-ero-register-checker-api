package uk.gov.dluhc.registercheckerapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.applicationsapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.messaging.MessageQueue
import java.util.UUID

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckResultPublisherTest {

    @Mock
    private lateinit var confirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var postalVoteConfirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var proxyVoteConfirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var overseasVoteConfirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    private lateinit var registerCheckResultPublisher: RegisterCheckResultPublisher

    @BeforeEach
    fun setup() {
        // not using @InjectMocks due to https://github.com/mockito/mockito/issues/2921
        registerCheckResultPublisher = RegisterCheckResultPublisher(
            postalVoteConfirmRegisterCheckResultQueue = postalVoteConfirmRegisterCheckResultMockQueue,
            proxyVoteConfirmRegisterCheckResultQueue = proxyVoteConfirmRegisterCheckResultMockQueue,
            overseasVoteConfirmRegisterCheckResultQueue = overseasVoteConfirmRegisterCheckResultMockQueue,
            confirmRegisterCheckResultQueue = confirmRegisterCheckResultMockQueue,
        )
    }

    @TestFactory
    fun `should publish the message to the relevant queue`() = listOf(
        SourceType.VOTER_MINUS_CARD to confirmRegisterCheckResultMockQueue,
        SourceType.POSTAL_MINUS_VOTE to postalVoteConfirmRegisterCheckResultMockQueue,
        SourceType.PROXY_MINUS_VOTE to proxyVoteConfirmRegisterCheckResultMockQueue,
        SourceType.OVERSEAS_MINUS_VOTE to overseasVoteConfirmRegisterCheckResultMockQueue
    ).map { (sourceType, targetQueue) ->
        DynamicTest.dynamicTest("result for $sourceType should be published to $targetQueue") {

            val expectedMessageContentSentToQueue = RegisterCheckResultMessage(
                sourceType = sourceType,
                sourceReference = "reference",
                sourceCorrelationId = UUID.randomUUID(),
                registerCheckResult = RegisterCheckResult.NO_MINUS_MATCH,
                matches = emptyList()
            )

            // when
            registerCheckResultPublisher.publish(expectedMessageContentSentToQueue)

            // Then
            verify(targetQueue).submit(expectedMessageContentSentToQueue)
        }
    }
}
