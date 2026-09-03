package com.petlink.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.error(code, ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            JsonProcessingException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.FILE_TOO_LARGE));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(DataIntegrityViolationException ex) {
        log.warn("Database constraint violation mapped to business conflict", ex);
        return ResponseEntity.status(ErrorCode.BUSINESS_STATE_CONFLICT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.BUSINESS_STATE_CONFLICT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        // Log internal details server-side only. Never expose stack traces/database details to clients.
        log.error("Unhandled server exception", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }
}
