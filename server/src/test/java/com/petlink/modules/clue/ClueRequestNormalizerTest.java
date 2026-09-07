package com.petlink.modules.clue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.clue.dto.CreateClueRequest;
import com.petlink.modules.clue.dto.UpdateClueRequest;
import com.petlink.modules.clue.service.ClueRequestNormalizer;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClueRequestNormalizerTest {
    private final ClueRequestNormalizer normalizer = new ClueRequestNormalizer();

    @Test
    void createTrimsTextNormalizesBlankSceneAndPreservesTokenOrder() {
        CreateClueRequest request = baseRequest();
        request.setLocation("  南京市玄武区  ");
        request.setSceneDescription("   ");
        request.setImageTokens(List.of(
                "550e8400-e29b-41d4-a716-446655440002",
                "550e8400-e29b-41d4-a716-446655440001"));

        var normalized = normalizer.normalizeCreate(1001L, request);
        assertEquals("南京市玄武区", normalized.getLocation());
        assertNull(normalized.getSceneDescription());
        assertEquals("550e8400-e29b-41d4-a716-446655440002", normalized.getImageTokens().get(0));
        assertEquals("550e8400-e29b-41d4-a716-446655440001", normalized.getImageTokens().get(1));
    }

    @Test
    void equivalentFoundTimeOffsetsProduceSameFingerprint() {
        CreateClueRequest a = baseRequest();
        CreateClueRequest b = baseRequest();
        a.setFoundTime(OffsetDateTime.parse("2026-08-29T08:20:00+08:00"));
        b.setFoundTime(OffsetDateTime.parse("2026-08-29T09:20:00+09:00"));
        assertEquals(normalizer.normalizeCreate(1001L, a).getFingerprint(),
                normalizer.normalizeCreate(1001L, b).getFingerprint());
    }

    @Test
    void duplicateImageTokensAreRejected() {
        CreateClueRequest request = baseRequest();
        request.setImageTokens(List.of(
                "550e8400-e29b-41d4-a716-446655440000",
                "550e8400-e29b-41d4-a716-446655440000"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> normalizer.normalizeCreate(1001L, request));
        assertEquals(ErrorCode.INVALID_PARAMETER, ex.getErrorCode());
    }

    @Test
    void foundTimeAllowsNormalClockSkewButRejectsRealFutureTime() {
        CreateClueRequest nearFuture=baseRequest();
        nearFuture.setFoundTime(OffsetDateTime.now().plusMinutes(1));
        assertDoesNotThrow(() -> normalizer.normalizeCreate(1001L,nearFuture));

        CreateClueRequest realFuture=baseRequest();
        realFuture.setFoundTime(OffsetDateTime.now().plusMinutes(10));
        BusinessException ex=assertThrows(BusinessException.class,() -> normalizer.normalizeCreate(1001L,realFuture));
        assertEquals(ErrorCode.INVALID_PARAMETER,ex.getErrorCode());
    }

    @Test
    void patchAllowsExplicitBlankSceneAsNullButRejectsEmptyPatch() {
        UpdateClueRequest patch = new UpdateClueRequest();
        patch.setSceneDescription("  ");
        var normalized = normalizer.normalizeUpdate(patch);
        assertTrue(normalized.isSceneDescriptionPresent());
        assertNull(normalized.getSceneDescription());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> normalizer.normalizeUpdate(new UpdateClueRequest()));
        assertEquals(ErrorCode.INVALID_PARAMETER, ex.getErrorCode());
    }

    private CreateClueRequest baseRequest() {
        CreateClueRequest request = new CreateClueRequest();
        request.setLocation("南京市玄武区");
        request.setFoundTime(OffsetDateTime.parse("2026-08-29T08:20:00+08:00"));
        request.setAnimalDescription("一只受伤的橘猫");
        request.setSceneDescription("躲在绿化带");
        request.setContact("13800138000");
        request.setImageTokens(List.of("550e8400-e29b-41d4-a716-446655440000"));
        return request;
    }
}
