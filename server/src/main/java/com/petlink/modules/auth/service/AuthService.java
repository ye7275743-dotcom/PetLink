package com.petlink.modules.auth.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.common.ValidationUtils;
import com.petlink.modules.auth.dto.LoginRequest;
import com.petlink.modules.auth.dto.RegisterRequest;
import com.petlink.modules.auth.dto.UpdateProfileRequest;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.modules.auth.mapper.SysUserMapper;
import com.petlink.modules.auth.vo.LoginResponse;
import com.petlink.modules.auth.vo.LoginUserResponse;
import com.petlink.modules.auth.vo.RegisterResponse;
import com.petlink.modules.auth.vo.UserProfileResponse;
import com.petlink.security.JwtService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        String account = ValidationUtils.normalizeAccount(request.getAccount());
        ValidationUtils.validatePassword(request.getPassword());
        String nickname = ValidationUtils.normalizeNicknameOrDefault(request.getNickname(), account);
        String phone = ValidationUtils.normalizePhone(request.getPhone());

        if (userMapper.findByAccount(account) != null) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        SysUser user = new SysUser();
        user.setAccount(account);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setRoleCode("USER");
        user.setStatus("ENABLED");
        // Do not depend on the database host's default timezone for new accounts.
        java.time.LocalDateTime now = java.time.LocalDateTime.now(TimeUtils.ZONE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        return new RegisterResponse(String.valueOf(user.getId()), user.getAccount(),
                user.getNickname(), user.getRoleCode());
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        String account = ValidationUtils.normalizeAccount(request.getAccount());
        ValidationUtils.validatePassword(request.getPassword());

        SysUser user = userMapper.findByAccount(account);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        JwtService.IssuedToken issued = jwtService.issue(user.getId());
        LoginUserResponse responseUser = new LoginUserResponse(
                String.valueOf(user.getId()), user.getAccount(), user.getNickname(),
                user.getPhone(), user.getRoleCode());
        return new LoginResponse(issued.getToken(), "Bearer", issued.getExpiresIn(),
                issued.getExpiresAt(), responseUser);
    }

    public UserProfileResponse getProfile(Long currentUserId) {
        return toProfile(requireEnabledUser(currentUserId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long currentUserId, UpdateProfileRequest request) {
        if (request == null || (!request.isNicknamePresent() && !request.isPhonePresent())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        requireEnabledUser(currentUserId);

        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, currentUserId);
        if (request.isNicknamePresent()) {
            update.set(SysUser::getNickname,
                    ValidationUtils.normalizeNicknameRequired(request.getNickname()));
        }
        if (request.isPhonePresent()) {
            // LambdaUpdateWrapper.set can explicitly persist NULL, matching Frozen PATCH semantics.
            update.set(SysUser::getPhone, ValidationUtils.normalizePhone(request.getPhone()));
        }
        userMapper.update(null, update);
        return toProfile(userMapper.selectById(currentUserId));
    }

    private SysUser requireEnabledUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    private UserProfileResponse toProfile(SysUser user) {
        return new UserProfileResponse(
                String.valueOf(user.getId()), user.getAccount(), user.getNickname(), user.getPhone(),
                user.getRoleCode(), user.getStatus(), TimeUtils.toOffset(user.getCreatedAt()),
                TimeUtils.toOffset(user.getUpdatedAt()));
    }
}
