package uk.gov.dluhc.registercheckerapi.config

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter

@Configuration
class JacksonConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper =
        JsonMapper.builder()
            .addModule(JavaTimeModule())
            .addModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .serializationInclusion(Include.NON_NULL)
            .build()

    @Bean
    fun jacksonMessageConverter(objectMapper: ObjectMapper): MappingJackson2MessageConverter =
        MappingJackson2MessageConverter().apply {
            this.serializedPayloadClass = String::class.java
            this.objectMapper = objectMapper
        }
}
