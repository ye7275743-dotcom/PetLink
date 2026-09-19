package com.petlink.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.modules.auth.mapper.SysUserMapper;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private JwtService jwtService;
    private SysUserMapper mapper;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        mapper = mock(SysUserMapper.class);
        filter = new JwtAuthenticationFilter(jwtService, mapper, new ObjectMapper());
        chain = mock(FilterChain.class);
    }

    @Test
    void expiredJwtReturns40102() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.parseUserId("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("40102"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void disabledAccountIsRejectedOnEveryRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.parseUserId("token")).thenReturn(7L);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus("DISABLED");
        user.setRoleCode("USER");
        when(mapper.selectById(7L)).thenReturn(user);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("40302"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void staleTokenDoesNotBlockPublicLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Authorization", "Bearer stale-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, mapper);
    }
}
