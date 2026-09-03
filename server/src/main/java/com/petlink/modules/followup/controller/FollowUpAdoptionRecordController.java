package com.petlink.modules.followup.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.followup.dto.SubmitFollowUpRequest;
import com.petlink.modules.followup.service.FollowUpService;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/adoption-records")
public class FollowUpAdoptionRecordController {
    private final FollowUpService service;
    public FollowUpAdoptionRecordController(FollowUpService service){this.service=service;}

    @PostMapping("/{adoptionRecordId}/follow-ups")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ResponseEntity<ApiResponse<FollowUpRecordResponse>> submit(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long adoptionRecordId,@RequestBody SubmitFollowUpRequest request){
        FollowUpService.SubmissionResult result=service.submit(principal,adoptionRecordId,request);
        return ResponseEntity.status(result.isCreated()?HttpStatus.CREATED:HttpStatus.OK).body(ApiResponse.success(result.getResponse()));
    }

    @GetMapping("/{adoptionRecordId}/follow-ups")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ApiResponse<List<FollowUpRecordResponse>> list(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long adoptionRecordId){
        return ApiResponse.success(service.byAdoptionRecord(principal,adoptionRecordId));
    }
}
