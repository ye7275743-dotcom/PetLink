package com.petlink.modules.adoption.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.adoption.dto.SubmitAdoptionApplicationRequest;
import com.petlink.modules.adoption.service.AdoptionService;
import com.petlink.modules.adoption.vo.AdoptionApplicationDetailResponse;
import com.petlink.modules.adoption.vo.AdoptionOverviewResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animals")
public class AnimalAdoptionController {
    private final AdoptionService service;
    public AnimalAdoptionController(AdoptionService service){this.service=service;}

    @PostMapping("/{animalId}/adoption-applications")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ResponseEntity<ApiResponse<AdoptionApplicationDetailResponse>> submit(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId,@RequestBody SubmitAdoptionApplicationRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.submit(principal,animalId,request)));
    }

    @GetMapping("/{animalId}/adoption-overview")
    @PreAuthorize("hasRole('RESCUER')")
    public ApiResponse<AdoptionOverviewResponse> overview(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId){
        return ApiResponse.success(service.overview(principal,animalId));
    }
}

