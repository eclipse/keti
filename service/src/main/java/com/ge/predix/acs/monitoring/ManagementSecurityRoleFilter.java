package com.ge.predix.acs.monitoring;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ManagementSecurityRoleFilter extends OncePerRequestFilter {

    private static final SimpleGrantedAuthority ROLE_FOR_MONITORING = new SimpleGrantedAuthority("acs.monitoring");

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequestWrapper wrappedHttpServletRequest = new HttpServletRequestWrapper(request) {
            @Override
            public boolean isUserInRole(final String role) {
                return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .contains(ROLE_FOR_MONITORING);
            }
        };
        filterChain.doFilter(wrappedHttpServletRequest, response);
    }
}
