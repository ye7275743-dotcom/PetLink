package com.petlink.modules.content.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.content.service.AnnouncementService;
import com.petlink.modules.content.vo.AnnouncementPublicDetailResponse;
import com.petlink.modules.content.vo.AnnouncementPublicSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementPublicController {
    private final AnnouncementService service;public AnnouncementPublicController(AnnouncementService service){this.service=service;}
    @GetMapping public ApiResponse<PageResponse<AnnouncementPublicSummaryResponse>> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.success(service.publicList(page,size));}
    @GetMapping("/{announcementId}") public ApiResponse<AnnouncementPublicDetailResponse> detail(@PathVariable Long announcementId){return ApiResponse.success(service.publicDetail(announcementId));}
}
