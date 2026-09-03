package com.petlink.modules.rescue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.rescue.dto.AnimalCreateRequest;
import com.petlink.modules.rescue.dto.RescueResultRequest;
import com.petlink.modules.rescue.service.RescueRequestNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RescueRequestNormalizerTest {
    private final RescueRequestNormalizer normalizer=new RescueRequestNormalizer();

    @Test void failedRequiresReasonAndForbidsAnimals() {
        RescueResultRequest request=new RescueResultRequest(); request.setResult("FAILED"); request.setFailureReason("  not found  ");
        var normalized=normalizer.normalizeResult(request);
        assertEquals("not found",normalized.getFailureReason());
        request.setAnimals(List.of());
        assertEquals(ErrorCode.INVALID_PARAMETER, assertThrows(BusinessException.class,()->normalizer.normalizeResult(request)).getErrorCode());
    }

    @Test void successRequiresAtLeastOneAnimal() {
        RescueResultRequest request=new RescueResultRequest(); request.setResult("SUCCESS"); request.setAnimals(List.of());
        assertEquals(ErrorCode.INVALID_PARAMETER, assertThrows(BusinessException.class,()->normalizer.normalizeResult(request)).getErrorCode());
    }

    @Test void successRejectsMoreThanNineImagesAcrossAnimals() {
        RescueResultRequest request=new RescueResultRequest(); request.setResult("SUCCESS");
        AnimalCreateRequest a=animal("A"); AnimalCreateRequest b=animal("B");
        a.setImageTokens(List.of("00000000-0000-0000-0000-000000000001","00000000-0000-0000-0000-000000000002","00000000-0000-0000-0000-000000000003","00000000-0000-0000-0000-000000000004","00000000-0000-0000-0000-000000000005"));
        b.setImageTokens(List.of("00000000-0000-0000-0000-000000000006","00000000-0000-0000-0000-000000000007","00000000-0000-0000-0000-000000000008","00000000-0000-0000-0000-000000000009","00000000-0000-0000-0000-000000000010"));
        request.setAnimals(List.of(a,b));
        assertEquals(ErrorCode.INVALID_PARAMETER, assertThrows(BusinessException.class,()->normalizer.normalizeResult(request)).getErrorCode());
    }

    @Test void successRejectsDuplicateTokenAcrossAnimals() {
        String token="00000000-0000-0000-0000-000000000001";
        RescueResultRequest request=new RescueResultRequest(); request.setResult("SUCCESS");
        AnimalCreateRequest a=animal("A"); a.setImageTokens(List.of(token));
        AnimalCreateRequest b=animal("B"); b.setImageTokens(List.of(token));
        request.setAnimals(List.of(a,b));
        assertEquals(ErrorCode.INVALID_PARAMETER, assertThrows(BusinessException.class,()->normalizer.normalizeResult(request)).getErrorCode());
    }

    @Test void animalFieldsAreNormalized() {
        RescueResultRequest request=new RescueResultRequest(); request.setResult("SUCCESS");
        AnimalCreateRequest a=animal(" Cat "); a.setColor("  orange  "); a.setInitialHealthRecord("  bandaged  ");
        request.setAnimals(List.of(a));
        var n=normalizer.normalizeResult(request).getAnimals().get(0);
        assertEquals("Cat",n.getName()); assertEquals("orange",n.getColor()); assertEquals("bandaged",n.getInitialHealthRecord());
    }

    private AnimalCreateRequest animal(String name) {
        AnimalCreateRequest a=new AnimalCreateRequest(); a.setName(name); a.setSpecies("CAT"); a.setSex("UNKNOWN"); a.setHealthCondition("stable"); return a;
    }
}
