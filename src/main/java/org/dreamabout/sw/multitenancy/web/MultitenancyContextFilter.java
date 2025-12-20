package org.dreamabout.sw.multitenancy.web;

import jakarta.servlet.*;
import org.dreamabout.sw.multitenancy.core.MultitenancyThreadContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MultitenancyContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        try {
            chain.doFilter(req, res);
        } finally {
            MultitenancyThreadContext.clear();
        }
    }
}
