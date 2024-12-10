package uk.gov.dluhc.registercheckerapi.messaging

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import uk.gov.dluhc.registercheckerapi.messaging.mapper.InitiateRegisterCheckMapper
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckService
import uk.gov.dluhc.registercheckerapi.service.ReplicationMessagingService
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildPendingRegisterCheckDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildInitiateRegisterCheckMessage

@ExtendWith(MockitoExtension::class)
class InitiateRegisterCheckMessageListenerTest {

    @Mock
    private lateinit var registerCheckService: RegisterCheckService

    @Mock
    private lateinit var mapper: InitiateRegisterCheckMapper

    @Mock
    private lateinit var replicationMessagingService: ReplicationMessagingService

    @InjectMocks
    private lateinit var objectUnderTest: InitiateRegisterCheckMessageListener

    @Test
    fun `should save and forward initiate register check message`() {
        // Given
        val payload = buildInitiateRegisterCheckMessage()
        val buildPendingRegisterCheckDto = buildPendingRegisterCheckDto()

        given(mapper.initiateCheckMessageToPendingRegisterCheckDto(any())).willReturn(buildPendingRegisterCheckDto)

        // When
        objectUnderTest.handleMessage(payload)

        // Then
        verify(mapper).initiateCheckMessageToPendingRegisterCheckDto(payload)
        verify(registerCheckService).save(any())
        verify(replicationMessagingService).forwardInitiateRegisterCheckMessage(payload)
    }
}
