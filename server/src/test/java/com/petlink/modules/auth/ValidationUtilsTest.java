package com.petlink.modules.auth;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.ValidationUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void accountShouldTrimButPasswordMustRemainRaw() {
        assertEquals("PetLink_01", ValidationUtils.normalizeAccount("  PetLink_01  "));
        assertDoesNotThrow(() -> ValidationUtils.validatePassword(" Example123!"));
    }

    @Test
    void passwordUtf8BytesMustNotExceed72() {
        String tooManyUtf8Bytes = "密".repeat(25); // 25 chars, 75 UTF-8 bytes
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ValidationUtils.validatePassword(tooManyUtf8Bytes));
        assertEquals(ErrorCode.INVALID_PASSWORD_FORMAT, ex.getErrorCode());
    }

    @Test
    void phoneBlankBecomesNullAndMainlandNumberIsAccepted() {
        assertNull(ValidationUtils.normalizePhone("   "));
        assertEquals("13800138000", ValidationUtils.normalizePhone(" 13800138000 "));
    }
}
