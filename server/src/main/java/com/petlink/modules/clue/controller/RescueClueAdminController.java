package com.petlink.modules.clue.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.dto.AuditClueRequest;
import com.petlink.modules.clue.service.RescueClueService;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rescue-clues")
@PreAuthorize("hasRole('ADMIN')")
public class RescueClueAdminController {
    private final RescueClueService service;

    public RescueClueAdminController(RescueClueService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<ClueSummaryResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.adminList(principal, page, size, status, keyword));
    }

    @PostMapping("/{clueId}/audit")
    public ApiResponse<StateActionResponse> audit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId,
            @RequestBody AuditClueRequest request) {
        return ApiResponse.success(service.audit(principal, clueId, request));
    }
}
