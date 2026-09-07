package com.petlink.modules.adoption.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.adoption.dto.AdoptionAuditRequest;
import com.petlink.modules.adoption.service.AdoptionService;
import com.petlink.modules.adoption.vo.AdoptionApplicationSummaryResponse;
import com.petlink.modules.adoption.vo.AdoptionAuditResponse;
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
@RequestMapping("/api/admin/adoption-applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdoptionAdminController {
    private final AdoptionService service;
    public AdoptionAdminController(AdoptionService service){this.service=service;}

    @GetMapping
    public ApiResponse<PageResponse<AdoptionApplicationSummaryResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                                               @RequestParam(defaultValue="1") int page,
                                                                               @RequestParam(defaultValue="20") int size,
                                                                               @RequestParam(required=false) String status,
                                                                               @RequestParam(required=false) Long animalId,
                                                                               @RequestParam(required=false) Long userId){
        return ApiResponse.success(service.adminApplications(principal,page,size,status,animalId,userId));
    }

    @PostMapping("/{applicationId}/audit")
    public ApiResponse<AdoptionAuditResponse> audit(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long applicationId,@RequestBody AdoptionAuditRequest request){
        return ApiResponse.success(service.audit(principal,applicationId,request));
    }
}
