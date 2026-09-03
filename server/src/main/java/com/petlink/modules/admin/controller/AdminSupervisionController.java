package com.petlink.modules.admin.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.admin.service.AdminSupervisionService;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.modules.animal.vo.AnimalSummaryResponse;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupervisionController {
    private final AdminSupervisionService service;
    public AdminSupervisionController(AdminSupervisionService service){this.service=service;}

    @GetMapping("/rescue-tasks")
    public ApiResponse<PageResponse<TaskSummaryResponse>> tasks(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String status,@RequestParam(required=false) Long rescuerId,
            @RequestParam(required=false) Long clueId){return ApiResponse.success(service.tasks(p,page,size,status,rescuerId,clueId));}

    @GetMapping("/animals")
    public ApiResponse<PageResponse<AnimalSummaryResponse>> animals(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String status,@RequestParam(required=false) String species,
            @RequestParam(required=false) Long rescueTaskId){return ApiResponse.success(service.animals(p,page,size,status,species,rescueTaskId));}

    @GetMapping("/adoption-records")
    public ApiResponse<PageResponse<AdoptionRecordResponse>> records(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) Long userId,@RequestParam(required=false) Long animalId,
            @RequestParam(required=false) Long applicationId){return ApiResponse.success(service.adoptionRecords(p,page,size,userId,animalId,applicationId));}
}
