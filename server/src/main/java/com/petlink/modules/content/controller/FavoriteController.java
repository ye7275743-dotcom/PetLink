package com.petlink.modules.content.controller;

import com.petlink.common.ApiResponse;
import com.petlink.common.PageResponse;
import com.petlink.modules.content.service.FavoriteService;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasAnyRole('USER','RESCUER')")
public class FavoriteController {
    private final FavoriteService service;public FavoriteController(FavoriteService service){this.service=service;}
    @GetMapping("/me") public ApiResponse<PageResponse<FavoriteAnimalResponse>> mine(@AuthenticationPrincipal UserPrincipal p,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.success(service.mine(p,page,size));}
}
