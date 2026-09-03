package com.petlink.modules.rescue.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.modules.rescue.dto.AddRescueRecordRequest;
import com.petlink.modules.rescue.dto.RescueResultRequest;
import com.petlink.modules.rescue.service.RescueTaskService;
import com.petlink.modules.rescue.vo.RescueRecordResponse;
import com.petlink.modules.rescue.vo.TaskDetailResponse;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rescue-tasks")
public class RescueTaskController {
    private final RescueTaskService service;
    public RescueTaskController(RescueTaskService service){ this.service=service; }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RESCUER')")
    public ApiResponse<PageResponse<TaskSummaryResponse>> mine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String status) {
        return ApiResponse.success(service.mine(principal,page,size,status));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ApiResponse<TaskDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId) {
        return ApiResponse.success(service.detail(principal,taskId));
    }

    @PostMapping("/{taskId}/start")
    @PreAuthorize("hasRole('RESCUER')")
    public ApiResponse<StateActionResponse> start(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId) {
        return ApiResponse.success(service.start(principal,taskId));
    }

    @PostMapping("/{taskId}/records")
    @PreAuthorize("hasRole('RESCUER')")
    public ResponseEntity<ApiResponse<RescueRecordResponse>> addRecord(
            @AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId,@RequestBody AddRescueRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.addRecord(principal,taskId,request)));
    }

    @GetMapping("/{taskId}/records")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ApiResponse<List<RescueRecordResponse>> records(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId) {
        return ApiResponse.success(service.records(principal,taskId));
    }

    @PostMapping("/{taskId}/result")
    @PreAuthorize("hasRole('RESCUER')")
    public ApiResponse<Object> result(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long taskId,@RequestBody RescueResultRequest request) {
        return ApiResponse.success(service.submitResult(principal,taskId,request));
    }
}
