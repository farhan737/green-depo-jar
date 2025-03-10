package com.example.demo.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserTypeAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserTypeAuthenticationFilter.class);

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        String userType = request.getParameter("userType");
        
        if (username == null) {
            username = "";
        }
        
        if (password == null) {
            password = "";
        }
        
        if (userType != null) {
            logger.debug("Setting userType in session: {}", userType);
            request.getSession().setAttribute("userType", userType);
        }
        
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        
        setDetails(request, authRequest);
        
        Authentication authentication = this.getAuthenticationManager().authenticate(authRequest);
        logger.debug("Authentication successful: {}", authentication);
        return authentication;
    }
    
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
        
        SecurityContextHolder.getContext().setAuthentication(authResult);
        logger.debug("Set SecurityContextHolder to {}", authResult);
        
        // Store authentication in the session
        request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        
        getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
    }
}
