package uk.gov.dluhc.registercheckerapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfiguration(
    @Value("\${dluhc.request.header.name}")
    private val requestHeaderName: String
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http.also { httpSecurity ->
            httpSecurity
                .sessionManagement {
                    it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                }
                .formLogin { it.disable() }
                .httpBasic { it.disable() }
                .authorizeRequests {
                    it.antMatchers(OPTIONS).permitAll()
                    it.antMatchers("/actuator/**").permitAll()
                    it.anyRequest().authenticated()
                }
                .addFilter(RegisterCheckerHeaderAuthenticationFilter(requestHeaderName))
        }.build()
    }
}
