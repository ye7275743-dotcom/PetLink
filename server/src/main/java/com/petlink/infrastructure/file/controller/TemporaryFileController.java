package com.petlink.infrastructure.file.controller;

import com.petlink.common.ApiResponse;
import com.petlink.infrastructure.file.dto.TemporaryUploadResponse;
import com.petlink.infrastructure.file.service.TemporaryFileService;
import com.petlink.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class TemporaryFileController {
    private final TemporaryFileService service;

    public TemporaryFileController(TemporaryFileService service) {
        this.service = service;
    }

    @PostMapping("/temporary")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ResponseEntity<ApiResponse<TemporaryUploadResponse>> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.upload(principal.getUserId(), file)));
    }
}
