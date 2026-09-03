package com.petlink.modules.clue;

import com.petlink.common.PageResponse;
import com.petlink.modules.clue.controller.RescueClueAdminController;
import com.petlink.modules.clue.controller.RescueClueController;
import com.petlink.modules.clue.service.RescueClueService;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.clue.vo.CreateClueResponse;
import com.petlink.modules.clue.vo.IdempotencyKeyResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({RescueClueController.class, RescueClueAdminController.class})
@AutoConfigureMockMvc(addFilters = false)
class RescueClueControllerWebTest {
    @Autowired MockMvc mvc;
    @MockBean RescueClueService service;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void issueIdempotencyKeyReturns201() throws Exception {
        when(service.issueIdempotencyKey(isNull())).thenReturn(new IdempotencyKeyResponse(
                "550e8400-e29b-41d4-a716-446655440000",
                OffsetDateTime.parse("2026-08-29T10:00:00+08:00")));
        mvc.perform(post("/api/rescue-clues/idempotency-keys"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotencyKey").value("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void createUsesFrozenIdempotencyHeaderAndReturns201() throws Exception {
        when(service.create(isNull(), eq("550e8400-e29b-41d4-a716-446655440000"), any()))
                .thenReturn(new CreateClueResponse("3001", "PENDING_REVIEW",
                        OffsetDateTime.parse("2026-08-29T09:45:00+08:00")));
        mvc.perform(post("/api/rescue-clues")
                        .header("Idempotency-Key", "550e8400-e29b-41d4-a716-446655440000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"南京\",\"foundTime\":\"2026-08-29T08:20:00+08:00\",\"animalDescription\":\"橘猫\",\"contact\":\"13800138000\",\"imageTokens\":[\"550e8400-e29b-41d4-a716-446655440001\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clueId").value("3001"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    @Test
    void mineKeepsPaginationNumbersNumeric() throws Exception {
        when(service.mine(isNull(), eq(1), eq(20), isNull()))
                .thenReturn(new PageResponse<ClueSummaryResponse>(List.of(), 1, 20, 0));
        mvc.perform(get("/api/rescue-clues/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void adminAuditUsesAdminResourcePath() throws Exception {
        when(service.audit(isNull(), eq(3001L), any())).thenReturn(new StateActionResponse(
                "3001", "WAITING_ACCEPT", OffsetDateTime.parse("2026-08-29T10:20:00+08:00")));
        mvc.perform(post("/api/admin/rescue-clues/3001/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_ACCEPT"));
    }
}
