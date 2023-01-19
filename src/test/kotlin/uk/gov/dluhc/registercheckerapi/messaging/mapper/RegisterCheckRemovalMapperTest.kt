package uk.gov.dluhc.registercheckerapi.messaging.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.registercheckerapi.dto.SourceType.VOTER_CARD
import uk.gov.dluhc.registercheckerapi.mapper.SourceTypeMapper
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging.buildRemoveRegisterCheckDataMessage

@ExtendWith(MockitoExtension::class)
internal class RegisterCheckRemovalMapperTest {

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private lateinit var mapper: RegisterCheckRemovalMapperImpl

    @Test
    fun `should map message to dto`() {
        // Given
        val message = buildRemoveRegisterCheckDataMessage()
        given(sourceTypeMapper.fromSqsToDtoEnum(any())).willReturn(VOTER_CARD)

        // When
        val actual = mapper.toRemovalDto(message)

        // Then
        assertThat(actual.sourceType).isEqualTo(VOTER_CARD)
        assertThat(actual.sourceReference).isEqualTo(message.sourceReference)

        verify(sourceTypeMapper).fromSqsToDtoEnum(SourceType.VOTER_MINUS_CARD)
        verifyNoMoreInteractions(sourceTypeMapper)
    }
}
