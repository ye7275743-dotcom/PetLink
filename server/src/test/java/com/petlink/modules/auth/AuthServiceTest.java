package com.petlink.modules.auth;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.auth.dto.LoginRequest;
import com.petlink.modules.auth.dto.RegisterRequest;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.modules.auth.mapper.SysUserMapper;
import com.petlink.modules.auth.service.AuthService;
import com.petlink.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private SysUserMapper mapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysUserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        service = new AuthService(mapper, passwordEncoder, jwtService);
    }

    @Test
    void publicRegistrationAlwaysCreatesUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setAccount(" demo_user ");
        request.setPassword("Example123!");
        request.setNickname(" ");
        request.setPhone("");

        when(mapper.findByAccount("demo_user")).thenReturn(null);
        when(passwordEncoder.encode("Example123!")).thenReturn("$2a$10$hash");
        doAnswer(invocation -> {
            SysUser u = invocation.getArgument(0);
            u.setId(1001L);
            return 1;
        }).when(mapper).insert(any(SysUser.class));

        var response = service.register(request);
        assertEquals("1001", response.getUserId());
        assertEquals("demo_user", response.getNickname());
        assertEquals("USER", response.getRoleCode());
    }

    @Test
    void disabledAccountCannotLogin() {
        LoginRequest request = new LoginRequest();
        request.setAccount("admin");
        request.setPassword("UnitTestPassword123!");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setAccount("admin");
        user.setPasswordHash("hash");
        user.setStatus("DISABLED");
        user.setRoleCode("ADMIN");
        when(mapper.findByAccount("admin")).thenReturn(user);
        when(passwordEncoder.matches("UnitTestPassword123!", "hash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        verify(jwtService, never()).issue(any());
    }

    @Test
    void enabledAccountUsesJwtWithOnlyCurrentUserIdAsIdentityInput() {
        LoginRequest request = new LoginRequest();
        request.setAccount("user01");
        request.setPassword("Example123!");

        SysUser user = new SysUser();
        user.setId(1001L);
        user.setAccount("user01");
        user.setNickname("小明");
        user.setPhone("13800138000");
        user.setPasswordHash("hash");
        user.setStatus("ENABLED");
        user.setRoleCode("USER");
        when(mapper.findByAccount("user01")).thenReturn(user);
        when(passwordEncoder.matches("Example123!", "hash")).thenReturn(true);
        when(jwtService.issue(1001L)).thenReturn(new JwtService.IssuedToken(
                "token", 7200, OffsetDateTime.parse("2026-08-30T12:00:00+08:00")));

        var response = service.login(request);
        assertEquals("token", response.getAccessToken());
        assertEquals("USER", response.getUser().getRoleCode());
        verify(jwtService).issue(1001L);
    }

    @Test
    void duplicateRegistrationReturns40904() {
        RegisterRequest request = new RegisterRequest();
        request.setAccount("user01");
        request.setPassword("Example123!");
        SysUser existing = new SysUser();
        existing.setId(1L);
        when(mapper.findByAccount("user01")).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(request));
        assertEquals(ErrorCode.ACCOUNT_ALREADY_EXISTS, ex.getErrorCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    void emptyPatchBodyIsRejected() {
        com.petlink.modules.auth.dto.UpdateProfileRequest request = new com.petlink.modules.auth.dto.UpdateProfileRequest();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateProfile(1L, request));
        assertEquals(ErrorCode.INVALID_PARAMETER, ex.getErrorCode());
    }
}
