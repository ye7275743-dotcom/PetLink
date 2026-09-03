package com.petlink.modules.adoption.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.adoption.service.AdoptionService;
import com.petlink.modules.adoption.vo.AdoptionApplicationDetailResponse;
import com.petlink.modules.adoption.vo.AdoptionApplicationSummaryResponse;
import com.petlink.modules.adoption.vo.AdoptionStateActionResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adoption-applications")
public class AdoptionApplicationController {
    private final AdoptionService service;
    public AdoptionApplicationController(AdoptionService service){this.service=service;}

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<PageResponse<AdoptionApplicationSummaryResponse>> mine(@AuthenticationPrincipal UserPrincipal principal,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String status){
        return ApiResponse.success(service.myApplications(principal,page,size,status));
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ApiResponse<AdoptionApplicationDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long applicationId){
        return ApiResponse.success(service.applicationDetail(principal,applicationId));
    }

    @PostMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<AdoptionStateActionResponse> withdraw(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long applicationId){
        return ApiResponse.success(service.withdraw(principal,applicationId));
    }
}

