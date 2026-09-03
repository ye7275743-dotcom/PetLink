package com.petlink.modules.rescue.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.rescue.service.RescueTaskService;
import com.petlink.modules.rescue.vo.AcceptTaskResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescue-clues")
public class RescueTaskIntakeController {
    private final RescueTaskService service;
    public RescueTaskIntakeController(RescueTaskService service){ this.service=service; }

    @GetMapping("/waiting-acceptance")
    @PreAuthorize("hasRole('RESCUER')")
    public ApiResponse<PageResponse<ClueSummaryResponse>> waitingAcceptance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(service.waitingAcceptance(principal,page,size));
    }

    @PostMapping("/{clueId}/accept")
    @PreAuthorize("hasRole('RESCUER')")
    public ResponseEntity<ApiResponse<AcceptTaskResponse>> accept(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.accept(principal,clueId)));
    }
}
