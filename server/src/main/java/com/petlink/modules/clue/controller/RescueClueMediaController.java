package com.petlink.modules.clue.controller;

import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.clue.service.RescueClueMediaService;
import com.petlink.security.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media/rescue-clue-images")
public class RescueClueMediaController {
    private final RescueClueMediaService service;

    public RescueClueMediaController(RescueClueMediaService service) {
        this.service = service;
    }

    @GetMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long imageId) {
        FileStorageService.ImageBinary image = service.read(principal, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getBytes());
    }
}
