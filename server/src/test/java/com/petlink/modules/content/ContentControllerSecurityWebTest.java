package com.petlink.modules.content;

import com.petlink.config.CorsProperties;
import com.petlink.config.SecurityConfig;
import com.petlink.modules.auth.mapper.SysUserMapper;
import com.petlink.modules.content.controller.AnimalFavoriteController;
import com.petlink.modules.content.controller.AnnouncementAdminController;
import com.petlink.modules.content.controller.AnnouncementPublicController;
import com.petlink.modules.content.controller.FavoriteController;
import com.petlink.modules.content.service.AnnouncementService;
import com.petlink.modules.content.service.FavoriteService;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.modules.content.vo.FavoriteAnimalSummaryResponse;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AnimalFavoriteController.class,FavoriteController.class,AnnouncementPublicController.class,AnnouncementAdminController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties="petlink.cors.allowed-origins[0]=http://localhost:5173")
class ContentControllerSecurityWebTest {
    @Autowired MockMvc mvc;@MockBean FavoriteService favoriteService;@MockBean AnnouncementService announcementService;@MockBean JwtService jwtService;@MockBean SysUserMapper sysUserMapper;
    private static final String ANNOUNCEMENT="{\"title\":\"Title\",\"content\":\"Content\"}";
    @Test void publicAnnouncementListAndDetailPermitAnonymous() throws Exception {mvc.perform(get("/api/announcements")).andExpect(status().isOk());mvc.perform(get("/api/announcements/1")).andExpect(status().isOk());}
    @Test void favoriteRoutesRequireAuthentication() throws Exception {mvc.perform(post("/api/animals/1/favorite")).andExpect(status().isUnauthorized());mvc.perform(delete("/api/animals/1/favorite")).andExpect(status().isUnauthorized());mvc.perform(get("/api/favorites/me")).andExpect(status().isUnauthorized());}
    @Test @WithMockUser(roles="USER") void firstFavoriteReturns201() throws Exception {when(favoriteService.favorite(isNull(),eq(1L))).thenReturn(new FavoriteService.FavoriteResult(true,mockResponse()));mvc.perform(post("/api/animals/1/favorite")).andExpect(status().isCreated());}
    @Test @WithMockUser(roles="RESCUER") void repeatedFavoriteReturns200() throws Exception {when(favoriteService.favorite(isNull(),eq(1L))).thenReturn(new FavoriteService.FavoriteResult(false,mockResponse()));mvc.perform(post("/api/animals/1/favorite")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="USER") void memberCanListAndDeleteFavorites() throws Exception {mvc.perform(get("/api/favorites/me")).andExpect(status().isOk());mvc.perform(delete("/api/animals/1/favorite")).andExpect(status().isOk());}
    @Test @WithMockUser(roles="ADMIN") void adminCannotUseMemberFavoriteRoutes() throws Exception {mvc.perform(post("/api/animals/1/favorite")).andExpect(status().isForbidden());mvc.perform(get("/api/favorites/me")).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="USER") void userCannotUseAnnouncementAdminRoutes() throws Exception {mvc.perform(get("/api/admin/announcements")).andExpect(status().isForbidden());mvc.perform(post("/api/admin/announcements").contentType(MediaType.APPLICATION_JSON).content(ANNOUNCEMENT)).andExpect(status().isForbidden());}
    @Test @WithMockUser(roles="ADMIN") void adminCanReachAllAnnouncementManagementRoutes() throws Exception {mvc.perform(post("/api/admin/announcements").contentType(MediaType.APPLICATION_JSON).content(ANNOUNCEMENT)).andExpect(status().isCreated());mvc.perform(get("/api/admin/announcements")).andExpect(status().isOk());mvc.perform(get("/api/admin/announcements/1")).andExpect(status().isOk());mvc.perform(patch("/api/admin/announcements/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"N\",\"version\":0}")).andExpect(status().isOk());mvc.perform(post("/api/admin/announcements/1/publish").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isOk());mvc.perform(post("/api/admin/announcements/1/withdraw").contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isOk());}
    @Test void malformedPublicPagingIsHandledByControllerAdvice() throws Exception {mvc.perform(get("/api/announcements?page=x")).andExpect(status().isBadRequest());}
    private FavoriteAnimalResponse mockResponse(){return new FavoriteAnimalResponse("8",OffsetDateTime.now(),new FavoriteAnimalSummaryResponse("1","Milo","CAT","UNKNOWN",12,"orange","good","AVAILABLE",null));}
}
