package com.example.vault.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class VaadinAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(VaadinAuthenticationSuccessHandler.class);
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        logger.info("Authentication successful for user: {}", authentication.getName());
        
        // Determine redirect URL
        String targetUrl = determineTargetUrl(request);
        logger.info("Redirecting to: {}", targetUrl);
        
        // Clear any authentication-related attributes
        clearAuthenticationAttributes(request);
        
        // Redirect to the target URL
        response.sendRedirect(targetUrl);
    }
    
    private String determineTargetUrl(HttpServletRequest request) {
        // Check if there's a saved request (from before login)
        String savedRequestUrl = getSavedRequestUrl(request);
        if (savedRequestUrl != null && !savedRequestUrl.contains("/login")) {
            return savedRequestUrl;
        }
        
        // Default to secrets page
        return request.getContextPath() + "/secrets";
    }
    
    private String getSavedRequestUrl(HttpServletRequest request) {
        // This would typically come from Spring Security's saved request
        // For now, we'll just return null and use the default
        return null;
    }
    
    private void clearAuthenticationAttributes(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession().removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        }
    }
}
