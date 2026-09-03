package com.petlink.modules.rescue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.config.CorsProperties;
import com.petlink.config.SecurityConfig;
import com.petlink.modules.auth.mapper.SysUserMapper;
import com.petlink.modules.rescue.controller.RescueTaskAdminController;
import com.petlink.modules.rescue.controller.RescueTaskController;
import com.petlink.modules.rescue.controller.RescueTaskIntakeController;
import com.petlink.modules.rescue.service.RescueTaskService;
import com.petlink.security.JwtService;
import com.petlink.security.RestAccessDeniedHandler;
import com.petlink.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RescueTaskIntakeController.class, RescueTaskController.class, RescueTaskAdminController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = "petlink.cors.allowed-origins[0]=http://localhost:5173")
class RescueTaskControllerSecurityWebTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    RescueTaskService service;

    // Keep the real JwtAuthenticationFilter in the web slice, but isolate its external collaborators.
    @MockBean
    JwtService jwtService;

    @MockBean
    SysUserMapper sysUserMapper;

    @Test
    void unauthenticatedAcceptReturns401() throws Exception {
        mvc.perform(post("/api/rescue-clues/3001/accept"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAcceptTask() throws Exception {
        mvc.perform(post("/api/rescue-clues/3001/accept"))
                .andExpect(status().isForbidden());
        verify(service, never()).accept(isNull(), eq(3001L));
    }

    @Test
    @WithMockUser(roles = "RESCUER")
    void nonOwnerRescuerGetsHiddenResourceNotFoundFromTaskDetail() throws Exception {
        when(service.detail(isNull(), eq(4001L))).thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mvc.perform(get("/api/rescue-tasks/4001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanViewTaskDetailAndRecords() throws Exception {
        mvc.perform(get("/api/rescue-tasks/4001"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/rescue-tasks/4001/records"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotStartRescuerTask() throws Exception {
        mvc.perform(post("/api/rescue-tasks/4001/start"))
                .andExpect(status().isForbidden());
        verify(service, never()).start(isNull(), eq(4001L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotAppendRescueRecord() throws Exception {
        mvc.perform(post("/api/rescue-tasks/4001/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test record\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotSubmitRescueResult() throws Exception {
        mvc.perform(post("/api/rescue-tasks/4001/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"FAILED\",\"failureReason\":\"not found\"}"))
                .andExpect(status().isForbidden());
    }
}
