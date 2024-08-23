package uk.gov.dluhc.registercheckerapi.testsupport.emails

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class LocalstackEmailMessages(
    val messages: List<LocalstackEmailMessage>,
)

data class LocalstackEmailMessage(
    @field:JsonProperty("Id")
    val id: String? = null,
    @field:JsonProperty("Timestamp")
    val timestamp: LocalDateTime,
    @field:JsonProperty("Region")
    val region: String? = null,
    @field:JsonProperty("Source")
    val source: String,
    @field:JsonProperty("Destination")
    val destination: LocalstackEmailDestination,
    @field:JsonProperty("Subject")
    val subject: String,
    @field:JsonProperty("Body")
    val body: LocalstackEmailBody,
)

data class LocalstackEmailBody(
    @field:JsonProperty("text_part")
    val textPart: String? = null,
    @field:JsonProperty("html_part")
    val htmlPart: String? = null,
)

data class LocalstackEmailDestination(
    @field:JsonProperty("ToAddresses")
    val toAddresses: Set<String>
)
