package com.petlink.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestCorrelationFilter.MDC_KEY);
    }

    @Test
    void propagatesSafeRequestIdToResponseAndLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "ui-task-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> assertEquals("ui-task-42", MDC.get(RequestCorrelationFilter.MDC_KEY));
        filter.doFilter(request, response, chain);

        assertEquals("ui-task-42", response.getHeader(RequestCorrelationFilter.HEADER));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeOrOversizedIdsWithGeneratedValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "bad id\r\nX-Evil: yes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String id = response.getHeader(RequestCorrelationFilter.HEADER);
        assertNotNull(id);
        assertTrue(id.matches("[0-9a-f-]{36}"));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }
}
