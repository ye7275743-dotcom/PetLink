package com.petlink.modules.followup;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.followup.dto.SubmitFollowUpRequest;
import com.petlink.modules.followup.service.FollowUpRequestNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpRequestNormalizerTest {
    private final FollowUpRequestNormalizer normalizer=new FollowUpRequestNormalizer();
    private static final String KEY="61f3576a-432f-4e08-8b69-642d3a6776d9";
    private static final String TOKEN="550e8400-e29b-41d4-a716-446655440030";

    @Test void idempotencyKeyIsCanonicalizedToLowercase(){SubmitFollowUpRequest r=req();r.setIdempotencyKey(KEY.toUpperCase());assertEquals(KEY,normalizer.normalizeIdempotencyKey(r));}
    @Test void malformedIdempotencyKeyUses40005(){SubmitFollowUpRequest r=req();r.setIdempotencyKey("bad");assertEquals(ErrorCode.INVALID_IDEMPOTENCY_KEY,assertThrows(BusinessException.class,()->normalizer.normalizeIdempotencyKey(r)).getErrorCode());}
    @Test void blankTextNormalizesToNull(){SubmitFollowUpRequest r=req();r.setContent("   ");r.setHealthCondition(" ok ");var n=normalizer.normalizeForCreate(r,KEY);assertNull(n.getContent());assertEquals("ok",n.getHealthCondition());}
    @Test void allEmptyIs40001(){SubmitFollowUpRequest r=req();r.setContent(" ");r.setHealthCondition(null);r.setImageTokens(List.of());assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->normalizer.normalizeForCreate(r,KEY)).getErrorCode());}
    @Test void imagesAloneAreValidContent(){SubmitFollowUpRequest r=req();r.setContent(null);r.setHealthCondition(null);r.setImageTokens(List.of(TOKEN));assertEquals(List.of(TOKEN),normalizer.normalizeForCreate(r,KEY).getImageTokens());}
    @Test void imageTokensAreCanonicalizedAndKeepClientOrder(){String t2="550e8400-e29b-41d4-a716-446655440031";SubmitFollowUpRequest r=req();r.setImageTokens(List.of(t2.toUpperCase(),TOKEN));assertEquals(List.of(t2,TOKEN),normalizer.normalizeForCreate(r,KEY).getImageTokens());}
    @Test void duplicateImageTokenIsRejected(){SubmitFollowUpRequest r=req();r.setImageTokens(List.of(TOKEN,TOKEN));assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->normalizer.normalizeForCreate(r,KEY)).getErrorCode());}
    @Test void moreThanNineImagesIsRejected(){SubmitFollowUpRequest r=req();r.setImageTokens(List.of("550e8400-e29b-41d4-a716-446655440001","550e8400-e29b-41d4-a716-446655440002","550e8400-e29b-41d4-a716-446655440003","550e8400-e29b-41d4-a716-446655440004","550e8400-e29b-41d4-a716-446655440005","550e8400-e29b-41d4-a716-446655440006","550e8400-e29b-41d4-a716-446655440007","550e8400-e29b-41d4-a716-446655440008","550e8400-e29b-41d4-a716-446655440009","550e8400-e29b-41d4-a716-446655440010"));assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->normalizer.normalizeForCreate(r,KEY)).getErrorCode());}
    @Test void overlongContentIsRejected(){SubmitFollowUpRequest r=req();r.setContent("x".repeat(2001));assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,()->normalizer.normalizeForCreate(r,KEY)).getErrorCode());}

    private SubmitFollowUpRequest req(){SubmitFollowUpRequest r=new SubmitFollowUpRequest();r.setIdempotencyKey(KEY);r.setContent("content");r.setImageTokens(List.of());return r;}
}
