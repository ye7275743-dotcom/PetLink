package com.petlink.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_PARAMETER(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    INVALID_ACCOUNT_FORMAT(40002, HttpStatus.BAD_REQUEST, "账号格式不合法"),
    INVALID_PASSWORD_FORMAT(40003, HttpStatus.BAD_REQUEST, "密码格式不合法"),
    INVALID_FILE(40004, HttpStatus.BAD_REQUEST, "文件不合法"),
    INVALID_IDEMPOTENCY_KEY(40005, HttpStatus.BAD_REQUEST, "幂等键不合法"),

    UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "未认证"),
    TOKEN_EXPIRED(40102, HttpStatus.UNAUTHORIZED, "登录凭证已过期"),
    INVALID_CREDENTIALS(40103, HttpStatus.UNAUTHORIZED, "账号或密码错误"),

    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无权限执行该操作"),
    ACCOUNT_DISABLED(40302, HttpStatus.FORBIDDEN, "账号已禁用"),

    RESOURCE_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),

    BUSINESS_STATE_CONFLICT(40901, HttpStatus.CONFLICT, "业务状态冲突"),
    DUPLICATE_OPERATION(40902, HttpStatus.CONFLICT, "重复操作"),
    OPTIMISTIC_LOCK_CONFLICT(40903, HttpStatus.CONFLICT, "数据已被其他请求修改"),
    ACCOUNT_ALREADY_EXISTS(40904, HttpStatus.CONFLICT, "账号已存在"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(40905, HttpStatus.CONFLICT, "幂等请求正在处理中"),
    IDEMPOTENCY_KEY_REUSED(40906, HttpStatus.CONFLICT, "幂等键已被其他请求使用"),
    ADOPTION_APPLICATION_ALREADY_EXISTS(40907, HttpStatus.CONFLICT, "该用户已申请过此动物"),
    IDEMPOTENCY_KEY_CONFLICT(40908, HttpStatus.CONFLICT, "幂等键与原请求冲突"),

    FILE_TOO_LARGE(41301, HttpStatus.PAYLOAD_TOO_LARGE, "文件过大"),
    INTERNAL_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getDefaultMessage() { return defaultMessage; }
}
