package uk.gov.dluhc.registercheckerapi.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class RegisterCheckerHeaderAuthenticationFilter : RequestHeaderAuthenticationFilter() {

    private val clientCertSerialHeader = "client-cert-serial"

    init {
        setPrincipalRequestHeader(clientCertSerialHeader)
        // This setting returns an HTTP 404 with no error message instead of an
        // HTTP 500 with the "missing header" exception message
        setExceptionIfHeaderMissing(false)
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain?
    ) {
        val requestHeaderValue = super.getPreAuthenticatedPrincipal(servletRequest as HttpServletRequest?) as String?

        if (requestHeaderValue?.isNotBlank() == true) {
            val authToken = PreAuthenticatedAuthenticationToken(requestHeaderValue, requestHeaderValue, AUTHORITIES)
            logger.info("Setting authenticated auth token $authToken to context ")
            SecurityContextHolder.getContext().authentication = authToken
        } else {
            logger.error("$clientCertSerialHeader header not present in request header")
        }

        filterChain?.doFilter(servletRequest, servletResponse)
    }

    companion object {
        private val AUTHORITIES: MutableList<GrantedAuthority> = ArrayList()

        init {
            AUTHORITIES.add(SimpleGrantedAuthority("ROLE_USER")) // TODO What should be the role TBD?
        }
    }
}
