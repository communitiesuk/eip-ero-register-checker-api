package uk.gov.dluhc.registercheckerapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
    @Value("\${dluhc.request.header.name}")
    private val requestHeaderName: String,
) {

    companion object {
        private val BYPASS_URLS_FOR_REQUEST_HEADER_AUTHENTICATION = listOf("/actuator/", "/admin/pending-checks/**")
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .cors { }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(OPTIONS).permitAll()
                it.requestMatchers("/actuator/**").permitAll()
                // These requests are authenticated through the API gateway using IAM
                it.requestMatchers("/admin/pending-checks/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilter(RegisterCheckerHeaderAuthenticationFilter(requestHeaderName, BYPASS_URLS_FOR_REQUEST_HEADER_AUTHENTICATION))
            .build()
}
