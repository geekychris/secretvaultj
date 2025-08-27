package com.example.vault.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        
        // Log the incoming request
        logger.info("=== REQUEST: {} {} ===", method, uri);
        logger.debug("User-Agent: {}", userAgent);
        logger.debug("Remote Address: {}", request.getRemoteAddr());
        
        // Log authentication status before processing
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.debug("Authentication present: {} (authenticated: {})", auth.getName(), auth.isAuthenticated());
        } else {
            logger.debug("No authentication in SecurityContext");
        }
        
        // Process the request
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log the response
            logger.info("=== RESPONSE: {} {} -> {} ({} ms) ===", method, uri, response.getStatus(), duration);
            
            // Log authentication status after processing
            Authentication postAuth = SecurityContextHolder.getContext().getAuthentication();
            if (postAuth != null) {
                logger.debug("Post-request authentication: {} (authenticated: {})", postAuth.getName(), postAuth.isAuthenticated());
            } else {
                logger.debug("No authentication in SecurityContext after request");
            }
        }
    }
}
