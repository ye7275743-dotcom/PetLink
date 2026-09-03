package com.petlink.common;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern ACCOUNT = Pattern.compile("^[A-Za-z0-9_]{4,50}$");
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private ValidationUtils() {}

    public static String normalizeAccount(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_FORMAT);
        }
        String value = raw.trim();
        if (!ACCOUNT.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_FORMAT);
        }
        return value;
    }

    public static void validatePassword(String password) {
        if (password == null) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        int chars = password.codePointCount(0, password.length());
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (chars < 8 || chars > 64 || bytes > 72) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    public static String normalizeNicknameOrDefault(String nickname, String account) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return account;
        }
        return normalizeNicknameRequired(nickname);
    }

    public static String normalizeNicknameRequired(String nickname) {
        if (nickname == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "昵称不能为空");
        }
        String value = nickname.trim();
        int chars = value.codePointCount(0, value.length());
        if (chars < 1 || chars > 50) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "昵称长度必须为 1～50 个字符");
        }
        return value;
    }

    public static String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String value = phone.trim();
        if (value.length() > 20 || !PHONE.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "手机号格式不合法");
        }
        return value;
    }
}
