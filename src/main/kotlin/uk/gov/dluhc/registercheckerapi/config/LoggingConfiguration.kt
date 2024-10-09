package uk.gov.dluhc.registercheckerapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.dluhc.logging.config.CorrelationIdMdcScheduledAspect
import uk.gov.dluhc.logging.rest.CorrelationIdMdcInterceptor
import uk.gov.dluhc.logging.rest.CorrelationIdRestTemplateClientHttpRequestInterceptor
import uk.gov.dluhc.logging.rest.CorrelationIdWebClientMdcExchangeFilter

@Configuration
class LoggingConfiguration : WebMvcConfigurer {

    @Override
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CorrelationIdMdcInterceptor())
    }

    @Bean
    fun correlationIdMdcScheduledAspect() = CorrelationIdMdcScheduledAspect()

    @Bean
    fun correlationIdRestTemplateClientHttpRequestInterceptor() =
        CorrelationIdRestTemplateClientHttpRequestInterceptor()

    @Bean
    fun correlationIdWebClientMdcExchangeFilter() = CorrelationIdWebClientMdcExchangeFilter()
}
