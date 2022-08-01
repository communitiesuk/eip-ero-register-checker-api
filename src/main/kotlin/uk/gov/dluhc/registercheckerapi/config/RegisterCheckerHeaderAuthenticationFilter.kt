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

class RegisterCheckerHeaderAuthenticationFilter(requestHeaderName: String) : RequestHeaderAuthenticationFilter() {

    private val clientCertSerialHeaderName = requestHeaderName

    companion object {
        private val AUTHORITIES: MutableList<GrantedAuthority> = ArrayList()

        init {
            AUTHORITIES.add(SimpleGrantedAuthority("ROLE_EMS_SYSTEM"))
        }
    }

    init {
        setPrincipalRequestHeader(requestHeaderName)
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

        if (requestHeaderValue.isNullOrBlank()) {
            logger.info("[$clientCertSerialHeaderName] header is not present in request header")
        } else {
            val authToken = PreAuthenticatedAuthenticationToken(clientCertSerialHeaderName, requestHeaderValue, AUTHORITIES)
            SecurityContextHolder.getContext().authentication = authToken
        }

        filterChain?.doFilter(servletRequest, servletResponse)
    }
}
