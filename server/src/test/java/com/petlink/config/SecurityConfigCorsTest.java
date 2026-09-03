package com.petlink.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCorsTest {

    @Test
    void preflightAllowsIdempotencyKeyForDevOrigin() throws Exception {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("http://localhost:5173"));
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/rescue-clues");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "Authorization, Content-Type, Idempotency-Key, X-Request-Id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CorsConfiguration configuration = source.getCorsConfiguration(request);
        assertNotNull(configuration);
        assertTrue(new DefaultCorsProcessor().processRequest(configuration, request, response));
        assertEquals("http://localhost:5173", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        String allowedHeaders = response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
        assertNotNull(allowedHeaders);
        assertTrue(allowedHeaders.toLowerCase().contains("idempotency-key"));
        assertTrue(allowedHeaders.toLowerCase().contains("x-request-id"));
        assertTrue(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS) == null
                || response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).toLowerCase().contains("x-request-id"));
    }
}
