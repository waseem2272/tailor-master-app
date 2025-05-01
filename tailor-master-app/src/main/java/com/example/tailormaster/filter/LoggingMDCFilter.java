package com.example.tailormaster.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoggingMDCFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String username = "ANONYMOUS";
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails userDetails) {
                    username = userDetails.getUsername();
                } else {
                    username = auth.getName();
                }
            }
            ThreadContext.put("user", username);
            chain.doFilter(request, response); // Continue the request
        } finally {
            ThreadContext.clearAll(); // Always clear MDC after the request to avoid data leakage
        }
    }
}
