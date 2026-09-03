package com.petlink.modules.followup.controller;

import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.followup.service.FollowUpMediaService;
import com.petlink.security.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media/follow-up-images")
public class FollowUpMediaController {
    private final FollowUpMediaService service;
    public FollowUpMediaController(FollowUpMediaService service){this.service=service;}
    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long imageId){
        FileStorageService.ImageBinary image=service.read(principal,imageId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.getContentType())).body(image.getBytes());
    }
}
