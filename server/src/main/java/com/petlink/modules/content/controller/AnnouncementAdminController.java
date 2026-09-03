package com.petlink.modules.content.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.content.dto.AnnouncementVersionRequest;
import com.petlink.modules.content.dto.CreateAnnouncementRequest;
import com.petlink.modules.content.dto.PatchAnnouncementRequest;
import com.petlink.modules.content.service.AnnouncementService;
import com.petlink.modules.content.vo.AnnouncementAdminDetailResponse;
import com.petlink.modules.content.vo.AnnouncementAdminSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
public class AnnouncementAdminController {
    private final AnnouncementService service;public AnnouncementAdminController(AnnouncementService service){this.service=service;}
    @PostMapping public ResponseEntity<ApiResponse<AnnouncementAdminDetailResponse>> create(@AuthenticationPrincipal UserPrincipal p,@RequestBody CreateAnnouncementRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(p,r)));}
    @GetMapping public ApiResponse<PageResponse<AnnouncementAdminSummaryResponse>> list(@AuthenticationPrincipal UserPrincipal p,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String status){return ApiResponse.success(service.adminList(p,page,size,status));}
    @GetMapping("/{announcementId}") public ApiResponse<AnnouncementAdminDetailResponse> detail(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long announcementId){return ApiResponse.success(service.adminDetail(p,announcementId));}
    @PatchMapping("/{announcementId}") public ApiResponse<AnnouncementAdminDetailResponse> patch(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long announcementId,@RequestBody PatchAnnouncementRequest r){return ApiResponse.success(service.patch(p,announcementId,r));}
    @PostMapping("/{announcementId}/publish") public ApiResponse<AnnouncementAdminDetailResponse> publish(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long announcementId,@RequestBody AnnouncementVersionRequest r){return ApiResponse.success(service.publish(p,announcementId,r));}
    @PostMapping("/{announcementId}/withdraw") public ApiResponse<AnnouncementAdminDetailResponse> withdraw(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long announcementId,@RequestBody AnnouncementVersionRequest r){return ApiResponse.success(service.withdraw(p,announcementId,r));}
}
