package uk.gov.dluhc.registercheckerapi.testsupport.emails

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * This class allows access to Localstack's list of sent emails.
 */
@Component
class EmailMessagesSentClient(
    private val objectMapper: ObjectMapper
) {

    fun getEmailMessagesSent(url: String): LocalstackEmailMessages {
        val webClient = WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
            }
            .build()

        val response = webClient.get().uri(URI.create(url))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(LocalstackEmailMessages::class.java)
            .onErrorResume { ex -> handleException(ex, "Error getting email messages from Localstack") }
            .block()

        return response!!
    }

    private fun handleException(ex: Throwable, message: String): Mono<out LocalstackEmailMessages>? {
        logger.error(ex) { "Unhandled exception thrown by WebClient" }
        return Mono.error(RuntimeException(message))
    }
}
