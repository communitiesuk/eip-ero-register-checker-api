package uk.gov.dluhc.registercheckerapi.config

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

class RegisterCheckerHeaderAuthenticationFilter(
    private val requestHeaderName: String,
    private val bypassRequestHeaderAuthenticationUrls: List<String>
) : RequestHeaderAuthenticationFilter() {

    companion object {
        private val AUTHORITIES = listOf(SimpleGrantedAuthority("ROLE_EMS_SYSTEM"))
    }

    init {
        setPrincipalRequestHeader(requestHeaderName)
        setExceptionIfHeaderMissing(false)
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain?
    ) {
        val requestHeaderValue = super.getPreAuthenticatedPrincipal(servletRequest as HttpServletRequest?) as String?
        val currentRequestUri = servletRequest?.requestURI
        val bypassAuthentication = bypassRequestHeaderAuthenticationUrls.any { currentRequestUri?.contains(it) ?: false }

        if (bypassAuthentication) {
            // do nothing
            logger.debug("Authentication not required for url:[$currentRequestUri]")
        } else {
            if (requestHeaderValue.isNullOrBlank()) {
                logger.info("[$requestHeaderName] header is not present in request header")
            } else {
                val authToken = PreAuthenticatedAuthenticationToken(requestHeaderName, requestHeaderValue, AUTHORITIES)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }
        filterChain?.doFilter(servletRequest, servletResponse)
    }
}
