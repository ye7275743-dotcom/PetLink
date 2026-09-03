package com.petlink.modules.adoption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlink.modules.adoption.dto.AdoptionAuditRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdoptionAuditRequestJsonTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void omittedRejectReasonIsDistinguishableFromExplicitNull() throws Exception {
        AdoptionAuditRequest omitted=mapper.readValue("{\"decision\":\"APPROVE\"}",AdoptionAuditRequest.class);assertFalse(omitted.isRejectReasonPresent());
        AdoptionAuditRequest explicit=mapper.readValue("{\"decision\":\"APPROVE\",\"rejectReason\":null}",AdoptionAuditRequest.class);assertTrue(explicit.isRejectReasonPresent());
    }
}

