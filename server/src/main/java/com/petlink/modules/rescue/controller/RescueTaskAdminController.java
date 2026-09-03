package com.petlink.modules.rescue.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.modules.rescue.dto.CancelRescueTaskRequest;
import com.petlink.modules.rescue.dto.FailureResolutionRequest;
import com.petlink.modules.rescue.service.RescueTaskService;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rescue-tasks")
@PreAuthorize("hasRole('ADMIN')")
public class RescueTaskAdminController {
    private final RescueTaskService service;
    public RescueTaskAdminController(RescueTaskService service){ this.service=service; }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<StateActionResponse> cancel(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId,
                                                   @RequestBody CancelRescueTaskRequest request) {
        return ApiResponse.success(service.cancel(principal,taskId,request));
    }

    @PostMapping("/{taskId}/failure-resolution")
    public ApiResponse<StateActionResponse> resolveFailure(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId,
                                                           @RequestBody FailureResolutionRequest request) {
        return ApiResponse.success(service.resolveFailure(principal,taskId,request));
    }
}
