package uk.gov.dluhc.registercheckerapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.dluhc.logging.rest.CorrelationIdMdcInterceptor

@Configuration
class WebMvcConfig(private val correlationIdMdcInterceptor: CorrelationIdMdcInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(correlationIdMdcInterceptor)
    }
}
