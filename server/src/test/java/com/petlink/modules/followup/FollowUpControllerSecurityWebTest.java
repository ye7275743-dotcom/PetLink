package com.petlink.modules.followup;

import com.petlink.config.CorsProperties;
import com.petlink.config.SecurityConfig;
import com.petlink.modules.auth.mapper.SysUserMapper;
import com.petlink.modules.followup.controller.FollowUpAdminController;
import com.petlink.modules.followup.controller.FollowUpAdoptionRecordController;
import com.petlink.modules.followup.controller.FollowUpController;
import com.petlink.modules.followup.controller.FollowUpMediaController;
import com.petlink.modules.followup.controller.FollowUpRescuerController;
import com.petlink.modules.followup.service.FollowUpMediaService;
import com.petlink.modules.followup.service.FollowUpService;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({FollowUpAdoptionRecordController.class,FollowUpController.class,FollowUpAdminController.class,FollowUpRescuerController.class,FollowUpMediaController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties="petlink.cors.allowed-origins[0]=http://localhost:5173")
class FollowUpControllerSecurityWebTest {
    @Autowired MockMvc mvc; @MockBean FollowUpService service; @MockBean FollowUpMediaService mediaService; @MockBean JwtService jwtService; @MockBean SysUserMapper sysUserMapper;
    private static final String BODY="{\"idempotencyKey\":\"61f3576a-432f-4e08-8b69-642d3a6776d9\",\"content\":\"ok\",\"imageTokens\":[]}";

    @Test void unauthenticatedSubmitReturns401() throws Exception {mvc.perform(post("/api/adoption-records/1/follow-ups").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());}
    @Test @WithMockUser(roles="USER") void userCanSubmitWith201() throws Exception {when(service.submit(isNull(),eq(1L),any())).thenReturn(new FollowUpService.SubmissionResult(response(),true));mvc.perform(post("/api/adoption-records/1/follow-ups").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());}
    @Test @WithMockUser(roles="USER") void replayCanReturn200() throws Exception {when(service.submit(isNull(),eq(1L),any())).thenReturn(new FollowUpService.SubmissionResult(response(),false));mvc.perform(post("/api/adoption-records/1/follow-ups").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());}
    @Test @WithMockUser(roles="ADMIN") void adminCannotSubmitFollowUp() throws Exception {mvc.perform(post("/api/adoption-records/1/follow-ups").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="USER") void userCanReachVisibleHistoryAndDetailRoutes() throws Exception {mvc.perform(get("/api/adoption-records/1/follow-ups")).andExpect(status().isOk());mvc.perform(get("/api/follow-ups/1")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="USER") void userCannotUseAdminOrRescuerPages() throws Exception {mvc.perform(get("/api/admin/follow-ups")).andExpect(status().isForbidden());mvc.perform(get("/api/rescuer/follow-ups")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="ADMIN") void adminCanUseAdminPage() throws Exception {mvc.perform(get("/api/admin/follow-ups")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="RESCUER") void rescuerCanUseRescuerPage() throws Exception {mvc.perform(get("/api/rescuer/follow-ups")).andExpect(status().isOk());}
    @Test void unauthenticatedFollowUpMediaReturns401() throws Exception {mvc.perform(get("/api/media/follow-up-images/1")).andExpect(status().isUnauthorized());}

    private FollowUpRecordResponse response(){return new FollowUpRecordResponse("8001","7001","1001","ok",null,List.of(),OffsetDateTime.now());}
}
