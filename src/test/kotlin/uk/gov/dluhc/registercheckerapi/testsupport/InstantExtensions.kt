package uk.gov.dluhc.registercheckerapi.testsupport

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

fun Instant.toRoundedUTCOffsetDateTime(): OffsetDateTime =
    this.atOffset(ZoneOffset.UTC).plus(Duration.ofMillis(500)).truncatedTo(ChronoUnit.SECONDS)
