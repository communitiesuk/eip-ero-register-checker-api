package uk.gov.dluhc.registercheckerapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.dluhc.logging.config.CorrelationIdMdcMessageListenerAspect
import uk.gov.dluhc.logging.rest.CorrelationIdMdcInterceptor
import uk.gov.dluhc.logging.rest.CorrelationIdRestTemplateClientHttpRequestInterceptor
import uk.gov.dluhc.logging.rest.CorrelationIdWebClientMdcExchangeFilter

@Configuration
class LoggingConfiguration {

    @Bean
    fun correlationIdMdcInterceptor() = CorrelationIdMdcInterceptor()

    @Bean
    fun correlationIdMdcMessageListenerAspect() = CorrelationIdMdcMessageListenerAspect()

    @Bean
    fun correlationIdRestTemplateClientHttpRequestInterceptor() =
        CorrelationIdRestTemplateClientHttpRequestInterceptor()

    @Bean
    fun correlationIdWebClientMdcExchangeFilter() = CorrelationIdWebClientMdcExchangeFilter()
}
