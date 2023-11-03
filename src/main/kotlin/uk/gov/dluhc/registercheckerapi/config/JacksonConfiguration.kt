package uk.gov.dluhc.registercheckerapi.config

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.TimeZone

@Configuration
class JacksonConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper =
        JsonMapper.builder()
            .addModule(JavaTimeModule().addDeserializer(OffsetDateTime::class.java, CustomOffsetDateTimeDeserializer))
            .addModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .serializationInclusion(Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()

    @Bean
    fun jacksonMessageConverter(objectMapper: ObjectMapper): MappingJackson2MessageConverter =
        MappingJackson2MessageConverter().apply {
            this.serializedPayloadClass = String::class.java
            this.objectMapper = objectMapper
        }

    // EROPSPT-190: IDOX were sending applicationCreatedAt as a local time, without timezone
    // information, and this cannot be parsed into an OffsetDateTime. If we receive such a
    // date-time, then we assume that it refers to a London local time.
    object CustomOffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OffsetDateTime {
            return try {
                InstantDeserializer.OFFSET_DATE_TIME.deserialize(p, ctxt)
            } catch (e: InvalidFormatException) {
                val localDateTime = LocalDateTimeDeserializer.INSTANCE.deserialize(p, ctxt)
                val offsetMillis = TimeZone.getTimeZone("Europe/London").getOffset(localDateTime.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli())
                localDateTime.atOffset(ZoneOffset.ofTotalSeconds(offsetMillis / 1000))
            }
        }
    }
}
