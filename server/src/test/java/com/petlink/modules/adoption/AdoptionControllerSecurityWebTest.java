package com.petlink.modules.adoption;

import com.petlink.config.CorsProperties;
import com.petlink.config.SecurityConfig;
import com.petlink.modules.adoption.controller.AdoptionAdminController;
import com.petlink.modules.adoption.controller.AdoptionApplicationController;
import com.petlink.modules.adoption.controller.AdoptionRecordController;
import com.petlink.modules.adoption.controller.AnimalAdoptionController;
import com.petlink.modules.adoption.service.AdoptionService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AnimalAdoptionController.class,AdoptionApplicationController.class,AdoptionAdminController.class,AdoptionRecordController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties="petlink.cors.allowed-origins[0]=http://localhost:5173")
class AdoptionControllerSecurityWebTest {
    @Autowired MockMvc mvc; @MockBean AdoptionService service; @MockBean JwtService jwtService; @MockBean SysUserMapper sysUserMapper;
    private static final String BODY="{\"adoptionReason\":\"r\",\"housingCondition\":\"h\",\"familyMembers\":\"f\",\"petExperience\":\"p\",\"contact\":\"c\"}";
    @Test void unauthenticatedSubmitReturns401() throws Exception {mvc.perform(post("/api/animals/1/adoption-applications").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());}
    @Test @WithMockUser(roles="USER") void userCanSubmitAndWithdraw() throws Exception {mvc.perform(post("/api/animals/1/adoption-applications").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());mvc.perform(post("/api/adoption-applications/1/withdraw")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="ADMIN") void adminCannotSubmitOrWithdraw() throws Exception {mvc.perform(post("/api/animals/1/adoption-applications").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());mvc.perform(post("/api/adoption-applications/1/withdraw")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="USER") void userCannotUseAdminAuditRoutes() throws Exception {mvc.perform(get("/api/admin/adoption-applications")).andExpect(status().isForbidden());mvc.perform(post("/api/admin/adoption-applications/1/audit").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\"}")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="ADMIN") void adminCanListAndAudit() throws Exception {mvc.perform(get("/api/admin/adoption-applications")).andExpect(status().isOk());mvc.perform(post("/api/admin/adoption-applications/1/audit").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\"}")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="RESCUER") void rescuerCanUseResponsibleOverview() throws Exception {mvc.perform(get("/api/animals/1/adoption-overview")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="USER") void userCannotUseResponsibleOverview() throws Exception {mvc.perform(get("/api/animals/1/adoption-overview")).andExpect(status().isForbidden());verify(service,never()).overview(isNull(),eq(1L));}
    @Test @WithMockUser(roles="ADMIN") void adminCannotUseResponsibleOverview() throws Exception {mvc.perform(get("/api/animals/1/adoption-overview")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="USER") void applicantCanReachOwnListDetailAndRecordsRoutes() throws Exception {mvc.perform(get("/api/adoption-applications/me")).andExpect(status().isOk());mvc.perform(get("/api/adoption-applications/1")).andExpect(status().isOk());mvc.perform(get("/api/adoption-records/me")).andExpect(status().isOk());mvc.perform(get("/api/adoption-records/1")).andExpect(status().isOk());}
}

