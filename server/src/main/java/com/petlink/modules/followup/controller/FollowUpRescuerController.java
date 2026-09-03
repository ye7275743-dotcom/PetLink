package com.petlink.modules.followup.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.followup.service.FollowUpService;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescuer/follow-ups")
@PreAuthorize("hasRole('RESCUER')")
public class FollowUpRescuerController {
    private final FollowUpService service;
    public FollowUpRescuerController(FollowUpService service){this.service=service;}
    @GetMapping
    public ApiResponse<PageResponse<FollowUpRecordResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) Long animalId){
        return ApiResponse.success(service.rescuerPage(principal,page,size,animalId));
    }
}
