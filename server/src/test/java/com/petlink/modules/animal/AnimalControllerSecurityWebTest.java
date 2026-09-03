package com.petlink.modules.animal;

import com.petlink.config.CorsProperties;
import com.petlink.config.SecurityConfig;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.animal.controller.AnimalController;
import com.petlink.modules.animal.controller.AnimalMediaController;
import com.petlink.modules.animal.service.AnimalMediaService;
import com.petlink.modules.animal.service.AnimalService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest({AnimalController.class,AnimalMediaController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties="petlink.cors.allowed-origins[0]=http://localhost:5173")
class AnimalControllerSecurityWebTest {
    @Autowired MockMvc mvc;
    @MockBean AnimalService service;
    @MockBean AnimalMediaService mediaService;
    @MockBean JwtService jwtService;
    @MockBean SysUserMapper sysUserMapper;

    @Test void visitorCanAccessPublicAnimalListDetailAndHealthRoute() throws Exception {
        mvc.perform(get("/api/animals")).andExpect(status().isOk());
        mvc.perform(get("/api/animals/1")).andExpect(status().isOk());
        mvc.perform(get("/api/animals/1/health-records")).andExpect(status().isOk());
        when(mediaService.read(isNull(), eq(1L)))
                .thenReturn(new FileStorageService.ImageBinary(new byte[]{1}, "image/png"));
        mvc.perform(get("/api/media/animal-images/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
    @Test void unauthenticatedMutationReturns401() throws Exception {
        mvc.perform(post("/api/animals/1/status-actions").contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"TO_OBSERVING\",\"version\":0}"))
                .andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(roles="USER") void userCannotModifyAnimal() throws Exception {
        mvc.perform(patch("/api/animals/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"version\":0}"))
                .andExpect(status().isForbidden());
        verify(service,never()).update(isNull(),eq(1L),any());
    }
    @Test @WithMockUser(roles="RESCUER") void rescuerCanAccessResponsibleRoute() throws Exception {
        mvc.perform(get("/api/animals/responsible/me")).andExpect(status().isOk());
    }
    @Test @WithMockUser(roles="ADMIN") void adminCannotUseRescuerResponsibleRoute() throws Exception {
        mvc.perform(get("/api/animals/responsible/me")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles="ADMIN") void adminCanUseMaintenanceRoutes() throws Exception {
        mvc.perform(patch("/api/animals/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"version\":0}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/animals/1/health-records").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"ok\"}"))
                .andExpect(status().isCreated());
    }
}
