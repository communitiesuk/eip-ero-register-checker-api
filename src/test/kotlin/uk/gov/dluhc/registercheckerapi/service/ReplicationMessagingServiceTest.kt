package uk.gov.dluhc.registercheckerapi.messaging

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.registercheckerapi.config.FeatureToggleConfiguration
import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckForwardingMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.PendingRegisterCheckArchiveMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RemoveRegisterCheckDataMessage
import uk.gov.dluhc.registercheckerapi.service.ReplicationMessagingService
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildInitiateRegisterCheckForwardingMessage
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRemoveRegisterCheckDataMessage
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReplicationMessagingServiceTest {

    @Mock
    private lateinit var initiateCheckMessageQueue: MessageQueue<InitiateRegisterCheckForwardingMessage>

    @Mock
    private lateinit var archiveRegisterCheckMessageQueue: MessageQueue<PendingRegisterCheckArchiveMessage>

    @Mock
    private lateinit var removeRegisterCheckDataMessageQueue: MessageQueue<RemoveRegisterCheckDataMessage>

    @Mock
    private lateinit var featureToggleConfiguration: FeatureToggleConfiguration

    // Inject mocks does not work in this case as all the message queues are seen as being the same type
    private val replicationMessagingService: ReplicationMessagingService by lazy {
        ReplicationMessagingService(
            initiateCheckMessageQueue,
            archiveRegisterCheckMessageQueue,
            removeRegisterCheckDataMessageQueue,
            featureToggleConfiguration,
        )
    }

    @Test
    fun `should forward initiate register check message if flag is on`() {
        // Given
        val request = buildInitiateRegisterCheckForwardingMessage()

        given(featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding).willReturn(true)

        // When
        replicationMessagingService.forwardInitiateRegisterCheckMessage(request)

        // Then
        verify(initiateCheckMessageQueue).submit(request)
    }

    @Test
    fun `should not forward initiate register check message if flag is off`() {
        // Given
        val request = buildInitiateRegisterCheckForwardingMessage()

        given(featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding).willReturn(false)

        // When
        replicationMessagingService.forwardInitiateRegisterCheckMessage(request)

        // Then
        verifyNoInteractions(initiateCheckMessageQueue)
    }

    @Test
    fun `should send archive register check message if flag is on`() {
        // Given
        val request = PendingRegisterCheckArchiveMessage(correlationId = UUID.randomUUID())

        given(featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding).willReturn(true)

        // When
        replicationMessagingService.sendArchiveRegisterCheckMessage(request)

        // Then
        verify(archiveRegisterCheckMessageQueue).submit(request)
    }

    @Test
    fun `should not send archive register check message if flag is off`() {
        // Given
        val request = PendingRegisterCheckArchiveMessage(correlationId = UUID.randomUUID())

        given(featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding).willReturn(false)

        // When
        replicationMessagingService.sendArchiveRegisterCheckMessage(request)

        // Then
        verifyNoInteractions(archiveRegisterCheckMessageQueue)
    }

    @Test
    fun `should forward remove register check message if flag is on`() {
        // Given
        val request = buildRemoveRegisterCheckDataMessage()

        given(featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding).willReturn(true)

        // When
        replicationMessagingService.forwardRemoveRegisterCheckDataMessage(request)

        // Then
        verify(removeRegisterCheckDataMessageQueue).submit(request)
    }

    @Test
    fun `should not forward remove register check message if flag is off`() {
        // Given
        val request = buildRemoveRegisterCheckDataMessage()

        given(featureToggleConfiguration.enableRegisterCheckToEmsMessageForwarding).willReturn(false)

        // When
        replicationMessagingService.forwardRemoveRegisterCheckDataMessage(request)

        // Then
        verifyNoInteractions(removeRegisterCheckDataMessageQueue)
    }
}
