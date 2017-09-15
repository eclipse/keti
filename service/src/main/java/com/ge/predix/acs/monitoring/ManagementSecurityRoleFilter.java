/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
