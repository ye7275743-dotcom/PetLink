package com.petlink.modules.admin;

import com.petlink.config.CorsProperties;
import com.petlink.config.SecurityConfig;
import com.petlink.modules.admin.controller.AdminStatsController;
import com.petlink.modules.admin.controller.AdminSupervisionController;
import com.petlink.modules.admin.controller.AdminUserController;
import com.petlink.modules.admin.service.AdminStatsService;
import com.petlink.modules.admin.service.AdminSupervisionService;
import com.petlink.modules.admin.service.AdminUserService;
import com.petlink.modules.auth.mapper.SysUserMapper;
import com.petlink.security.JwtService;
import com.petlink.security.RestAccessDeniedHandler;
import com.petlink.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminUserController.class,AdminSupervisionController.class,AdminStatsController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties="petlink.cors.allowed-origins[0]=http://localhost:5173")
class AdminControllerSecurityWebTest {
    @Autowired MockMvc mvc;@MockBean AdminUserService users;@MockBean AdminSupervisionService supervision;@MockBean AdminStatsService stats;@MockBean JwtService jwtService;@MockBean SysUserMapper sysUserMapper;
    @Test void anonymousCannotReachM08() throws Exception {mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());mvc.perform(get("/api/admin/stats/overview")).andExpect(status().isUnauthorized());}
    @Test @WithMockUser(roles="USER") void userCannotReachAnyM08Group() throws Exception {mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/rescue-tasks")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/animals")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/adoption-records")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/stats/overview")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="ADMIN") void adminCanReachAllTenRoutes() throws Exception {mvc.perform(get("/api/admin/users")).andExpect(status().isOk());mvc.perform(get("/api/admin/users/8")).andExpect(status().isOk());mvc.perform(post("/api/admin/users/8/enable")).andExpect(status().isOk());mvc.perform(post("/api/admin/users/8/disable")).andExpect(status().isOk());mvc.perform(post("/api/admin/users/8/promote-rescuer")).andExpect(status().isOk());mvc.perform(get("/api/admin/rescue-tasks")).andExpect(status().isOk());mvc.perform(get("/api/admin/animals")).andExpect(status().isOk());mvc.perform(get("/api/admin/adoption-records")).andExpect(status().isOk());mvc.perform(get("/api/admin/stats/overview")).andExpect(status().isOk());mvc.perform(get("/api/admin/stats/trends?from=2026-08-01&to=2026-08-02&granularity=DAY")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="RESCUER") void rescuerCannotUseM08() throws Exception {mvc.perform(get("/api/admin/rescue-tasks")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/stats/trends?from=2026-08-01&to=2026-08-02")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="ADMIN") void malformedQueryTypeReturns400() throws Exception {mvc.perform(get("/api/admin/users?page=x")).andExpect(status().isBadRequest());}
}
