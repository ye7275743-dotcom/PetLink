package com.petlink.modules.admin.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.admin.service.AdminUserService;
import com.petlink.modules.admin.vo.AdminUserActionResponse;
import com.petlink.modules.admin.vo.AdminUserDetailResponse;
import com.petlink.modules.admin.vo.AdminUserSummaryResponse;
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
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service){this.service=service;}

    @GetMapping
    public ApiResponse<PageResponse<AdminUserSummaryResponse>> list(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String roleCode,@RequestParam(required=false) String status,
            @RequestParam(required=false) String keyword){return ApiResponse.success(service.list(p,page,size,roleCode,status,keyword));}

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponse> detail(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long userId){return ApiResponse.success(service.detail(p,userId));}

    @PostMapping("/{userId}/enable")
    public ApiResponse<AdminUserActionResponse> enable(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long userId){return ApiResponse.success(service.enable(p,userId));}

    @PostMapping("/{userId}/disable")
    public ApiResponse<AdminUserActionResponse> disable(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long userId){return ApiResponse.success(service.disable(p,userId));}

    @PostMapping("/{userId}/promote-rescuer")
    public ApiResponse<AdminUserActionResponse> promote(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long userId){return ApiResponse.success(service.promoteRescuer(p,userId));}
}
