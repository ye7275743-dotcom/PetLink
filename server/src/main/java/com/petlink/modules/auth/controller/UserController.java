package com.petlink.modules.auth.controller;

import com.petlink.common.ApiResponse;
import com.petlink.modules.auth.dto.UpdateProfileRequest;
import com.petlink.modules.auth.service.AuthService;
import com.petlink.modules.auth.vo.UserProfileResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(authService.getProfile(principal.getUserId()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                                     @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(authService.updateProfile(principal.getUserId(), request));
    }
}
