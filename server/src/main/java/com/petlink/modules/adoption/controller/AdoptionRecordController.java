package com.petlink.modules.adoption.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.adoption.service.AdoptionService;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adoption-records")
public class AdoptionRecordController {
    private final AdoptionService service;
    public AdoptionRecordController(AdoptionService service){this.service=service;}

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<PageResponse<AdoptionRecordResponse>> mine(@AuthenticationPrincipal UserPrincipal principal,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){
        return ApiResponse.success(service.myRecords(principal,page,size));
    }

    @GetMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ApiResponse<AdoptionRecordResponse> detail(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long recordId){
        return ApiResponse.success(service.recordDetail(principal,recordId));
    }
}

