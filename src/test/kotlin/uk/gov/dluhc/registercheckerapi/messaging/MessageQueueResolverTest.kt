package uk.gov.dluhc.registercheckerapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType

@ExtendWith(MockitoExtension::class)
internal class MessageQueueResolverTest {

    @Mock
    private lateinit var confirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var postalVoteConfirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var proxyVoteConfirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var overseasVoteConfirmRegisterCheckResultMockQueue: MessageQueue<RegisterCheckResultMessage>

    @Mock
    private lateinit var registerCheckResultResponseQueue: MessageQueue<RegisterCheckResultMessage>

    private lateinit var messageQueueResolver: MessageQueueResolver

    @BeforeEach
    fun setup() {
        // not using @InjectMocks due to https://github.com/mockito/mockito/issues/2921
        messageQueueResolver = MessageQueueResolver(
            postalVoteConfirmRegisterCheckResultQueue = postalVoteConfirmRegisterCheckResultMockQueue,
            proxyVoteConfirmRegisterCheckResultQueue = proxyVoteConfirmRegisterCheckResultMockQueue,
            overseasVoteConfirmRegisterCheckResultQueue = overseasVoteConfirmRegisterCheckResultMockQueue,
            confirmRegisterCheckResultQueue = confirmRegisterCheckResultMockQueue,
            registerCheckResultResponseQueue = registerCheckResultResponseQueue

        )
    }

    @TestFactory
    fun `should publish the message to the relevant queue`() = listOf(
        SourceType.VOTER_MINUS_CARD to confirmRegisterCheckResultMockQueue,
        SourceType.POSTAL_MINUS_VOTE to postalVoteConfirmRegisterCheckResultMockQueue,
        SourceType.PROXY_MINUS_VOTE to proxyVoteConfirmRegisterCheckResultMockQueue,
        SourceType.OVERSEAS_MINUS_VOTE to overseasVoteConfirmRegisterCheckResultMockQueue,
        SourceType.APPLICATIONS_MINUS_API to registerCheckResultResponseQueue
    ).map { (sourceType, expectedTargetQueue) ->
        dynamicTest("for $sourceType return $expectedTargetQueue") {

            // When
            val actualResult = messageQueueResolver.getTargetQueueForSourceType(sourceType)

            // Then
            assertThat(actualResult).isEqualTo(expectedTargetQueue)
        }
    }
}
