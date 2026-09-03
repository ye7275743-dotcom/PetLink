package com.petlink.common;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request a safe, client-visible identifier and puts the same
 * value into the logging context.  This makes a user-facing error traceable
 * without exposing exception details or trusting arbitrary header content.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = normalize(request.getHeader(HEADER));
        String previous = MDC.get(MDC_KEY);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previous == null) MDC.remove(MDC_KEY);
            else MDC.put(MDC_KEY, previous);
        }
    }

    static String normalize(String value) {
        return value != null && SAFE_ID.matcher(value.trim()).matches()
                ? value.trim()
                : UUID.randomUUID().toString();
    }
}
