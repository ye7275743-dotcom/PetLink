package com.petlink.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlink.common.ApiResponse;
import com.petlink.common.ErrorCode;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.modules.auth.mapper.SysUserMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, SysUserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtService.parseUserId(token);
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }
            if (!"ENABLED".equals(user.getStatus())) {
                writeError(response, ErrorCode.ACCOUNT_DISABLED);
                return;
            }

            UserPrincipal principal = new UserPrincipal(user.getId(), user.getRoleCode());
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRoleCode());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            writeError(response, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            writeError(response, ErrorCode.UNAUTHORIZED);
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code));
    }
}
