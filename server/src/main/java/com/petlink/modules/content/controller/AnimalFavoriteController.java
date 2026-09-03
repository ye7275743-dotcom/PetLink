package com.petlink.modules.content.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.content.service.FavoriteService;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.modules.content.vo.FavoriteStateResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animals")
@PreAuthorize("hasAnyRole('USER','RESCUER')")
public class AnimalFavoriteController {
    private final FavoriteService service;public AnimalFavoriteController(FavoriteService service){this.service=service;}
    @PostMapping("/{animalId}/favorite")
    public ResponseEntity<ApiResponse<FavoriteAnimalResponse>> favorite(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long animalId){FavoriteService.FavoriteResult r=service.favorite(p,animalId);return ResponseEntity.status(r.isCreated()?HttpStatus.CREATED:HttpStatus.OK).body(ApiResponse.success(r.getResponse()));}
    @DeleteMapping("/{animalId}/favorite")
    public ApiResponse<FavoriteStateResponse> unfavorite(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long animalId){return ApiResponse.success(service.unfavorite(p,animalId));}
}
