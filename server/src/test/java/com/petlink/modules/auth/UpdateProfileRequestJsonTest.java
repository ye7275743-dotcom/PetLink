package com.petlink.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlink.modules.auth.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProfileRequestJsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void absentAndExplicitNullAreDistinguished() throws Exception {
        UpdateProfileRequest empty = mapper.readValue("{}", UpdateProfileRequest.class);
        assertFalse(empty.isNicknamePresent());
        assertFalse(empty.isPhonePresent());

        UpdateProfileRequest explicitNull = mapper.readValue("{\"phone\":null}", UpdateProfileRequest.class);
        assertTrue(explicitNull.isPhonePresent());
        assertNull(explicitNull.getPhone());
        assertFalse(explicitNull.isNicknamePresent());
    }
}
