package com.petlink.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlink.modules.auth.controller.AuthController;
import com.petlink.modules.auth.service.AuthService;
import com.petlink.modules.auth.vo.LoginResponse;
import com.petlink.modules.auth.vo.LoginUserResponse;
import com.petlink.modules.auth.vo.RegisterResponse;
import com.petlink.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerReturns201AndBigintAsString() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse("922337203685477580", "user01", "用户", "USER"));
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"user01\",\"password\":\"Example123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value("922337203685477580"))
                .andExpect(jsonPath("$.data.roleCode").value("USER"));
    }

    @Test
    void loginReturnsFrozenTokenShape() throws Exception {
        LoginUserResponse user = new LoginUserResponse("1001", "user01", "小明", null, "USER");
        when(authService.login(any())).thenReturn(new LoginResponse(
                "jwt-token", "Bearer", 7200, OffsetDateTime.parse("2026-08-30T12:00:00+08:00"), user));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"user01\",\"password\":\"Example123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200));
    }
}
