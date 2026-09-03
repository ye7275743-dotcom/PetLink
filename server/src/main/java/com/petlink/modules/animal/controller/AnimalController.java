package com.petlink.modules.animal.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.animal.dto.AddHealthRecordRequest;
import com.petlink.modules.animal.dto.AnimalStatusActionRequest;
import com.petlink.modules.animal.dto.AppendAnimalImagesRequest;
import com.petlink.modules.animal.dto.UpdateAnimalRequest;
import com.petlink.modules.animal.service.AnimalService;
import com.petlink.modules.animal.vo.AnimalDetailResponse;
import com.petlink.modules.animal.vo.AnimalSummaryResponse;
import com.petlink.modules.animal.vo.HealthRecordPublicResponse;
import com.petlink.modules.animal.vo.HealthRecordResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animals")
public class AnimalController {
    private final AnimalService service;
    public AnimalController(AnimalService service){this.service=service;}

    @GetMapping
    public ApiResponse<PageResponse<AnimalSummaryResponse>> publicList(
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String species,@RequestParam(required=false) String sex) {
        return ApiResponse.success(service.publicList(page,size,species,sex));
    }

    @GetMapping("/{animalId}")
    public ApiResponse<AnimalDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId) {
        return ApiResponse.success(service.detail(principal,animalId));
    }

    @GetMapping("/responsible/me")
    @PreAuthorize("hasRole('RESCUER')")
    public ApiResponse<PageResponse<AnimalSummaryResponse>> responsible(
            @AuthenticationPrincipal UserPrincipal principal,@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size,@RequestParam(required=false) String status) {
        return ApiResponse.success(service.responsible(principal,page,size,status));
    }

    @PatchMapping("/{animalId}")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ApiResponse<AnimalDetailResponse> update(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId,
                                                    @RequestBody UpdateAnimalRequest request) {
        return ApiResponse.success(service.update(principal,animalId,request));
    }

    @PostMapping("/{animalId}/images")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ResponseEntity<ApiResponse<AnimalDetailResponse>> addImages(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId,
                                                                        @RequestBody AppendAnimalImagesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.addImages(principal,animalId,request)));
    }

    @DeleteMapping("/{animalId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ApiResponse<AnimalDetailResponse> deleteImage(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId,
                                                          @PathVariable Long imageId) {
        return ApiResponse.success(service.deleteImage(principal,animalId,imageId));
    }

    @PostMapping("/{animalId}/health-records")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ResponseEntity<ApiResponse<HealthRecordResponse>> addHealthRecord(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId,
                                                                              @RequestBody AddHealthRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.addHealthRecord(principal,animalId,request)));
    }

    @GetMapping("/{animalId}/health-records")
    public ApiResponse<List<HealthRecordPublicResponse>> healthRecords(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId) {
        return ApiResponse.success(service.healthRecords(principal,animalId));
    }

    @PostMapping("/{animalId}/status-actions")
    @PreAuthorize("hasAnyRole('RESCUER','ADMIN')")
    public ApiResponse<AnimalDetailResponse> statusAction(@AuthenticationPrincipal UserPrincipal principal,@PathVariable Long animalId,
                                                           @RequestBody AnimalStatusActionRequest request) {
        return ApiResponse.success(service.statusAction(principal,animalId,request));
    }
}
