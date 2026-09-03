package com.petlink.modules.followup.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.followup.service.FollowUpService;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {
    private final FollowUpService service;
    public FollowUpController(FollowUpService service){this.service=service;}
    @GetMapping("/{followUpId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ApiResponse<FollowUpRecordResponse> detail(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long followUpId){
        return ApiResponse.success(service.detail(principal,followUpId));
    }
}
