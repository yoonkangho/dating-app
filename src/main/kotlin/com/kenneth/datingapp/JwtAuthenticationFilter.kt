package com.kenneth.datingapp

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Placeholder JWT filter for development. Extracts user ID from X-User-Id header.
 * Will be replaced with real JWT validation in issue #36.
 */
@Component
class JwtAuthenticationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val userId = request.getHeader("X-User-Id")
        if (userId != null) {
            try {
                val uuid = UUID.fromString(userId)
                val auth = UsernamePasswordAuthenticationToken(
                    uuid,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: IllegalArgumentException) {
                // Invalid UUID format, skip auth
            }
        }
        filterChain.doFilter(request, response)
    }
}
