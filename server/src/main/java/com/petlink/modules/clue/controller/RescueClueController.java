package com.petlink.modules.clue.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.dto.AppendClueImagesRequest;
import com.petlink.modules.clue.dto.CreateClueRequest;
import com.petlink.modules.clue.dto.UpdateClueRequest;
import com.petlink.modules.clue.service.RescueClueService;
import com.petlink.modules.clue.vo.ClueDetailResponse;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.clue.vo.CreateClueResponse;
import com.petlink.modules.clue.vo.IdempotencyKeyResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescue-clues")
public class RescueClueController {
    private final RescueClueService service;

    public RescueClueController(RescueClueService service) {
        this.service = service;
    }

    @PostMapping("/idempotency-keys")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ResponseEntity<ApiResponse<IdempotencyKeyResponse>> issueIdempotencyKey(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.issueIdempotencyKey(principal)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ResponseEntity<ApiResponse<CreateClueResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateClueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(principal, idempotencyKey, request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<PageResponse<ClueSummaryResponse>> mine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(service.mine(principal, page, size, status));
    }

    @GetMapping("/{clueId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER','ADMIN')")
    public ApiResponse<ClueDetailResponse> detail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId) {
        return ApiResponse.success(service.detail(principal, clueId));
    }

    @PatchMapping("/{clueId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<ClueDetailResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId,
            @RequestBody UpdateClueRequest request) {
        return ApiResponse.success(service.update(principal, clueId, request));
    }

    @PostMapping("/{clueId}/images")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ResponseEntity<ApiResponse<ClueDetailResponse>> appendImages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId,
            @RequestBody AppendClueImagesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.appendImages(principal, clueId, request)));
    }

    @DeleteMapping("/{clueId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<ClueDetailResponse> deleteImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId,
            @PathVariable Long imageId) {
        return ApiResponse.success(service.deleteImage(principal, clueId, imageId));
    }

    @PostMapping("/{clueId}/withdraw")
    @PreAuthorize("hasAnyRole('USER','RESCUER')")
    public ApiResponse<StateActionResponse> withdraw(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clueId) {
        return ApiResponse.success(service.withdraw(principal, clueId));
    }
}
