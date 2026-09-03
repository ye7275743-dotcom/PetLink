package com.petlink.modules.admin.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.admin.service.AdminStatsService;
import com.petlink.modules.admin.vo.AdminStatsOverviewResponse;
import com.petlink.modules.admin.vo.AdminStatsTrendsResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {
    private final AdminStatsService service;
    public AdminStatsController(AdminStatsService service){this.service=service;}

    @GetMapping("/overview")
    public ApiResponse<AdminStatsOverviewResponse> overview(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.success(service.overview(p));}

    @GetMapping("/trends")
    public ApiResponse<AdminStatsTrendsResponse> trends(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam String from,@RequestParam String to,@RequestParam(defaultValue="DAY") String granularity){return ApiResponse.success(service.trends(p,from,to,granularity));}
}
