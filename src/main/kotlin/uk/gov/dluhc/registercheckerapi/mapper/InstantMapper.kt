package uk.gov.dluhc.registercheckerapi.mapper

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class InstantMapper {

    fun toOffsetDateTime(instant: Instant?): OffsetDateTime? = instant?.atOffset(ZoneOffset.UTC)

    fun toInstant(offsetDateTime: OffsetDateTime?): Instant? = offsetDateTime?.toInstant()
}
