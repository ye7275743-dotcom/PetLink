package com.petlink.modules.rescue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.rescue.dto.AnimalCreateRequest;
import com.petlink.modules.rescue.dto.RescueResultRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class RescueRequestNormalizer {
    private static final Set<String> SEXES = Set.of("MALE", "FEMALE", "UNKNOWN");

    public String requiredText(String raw, int max) {
        if (raw == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String value = raw.trim();
        if (value.isEmpty() || value.length() > max) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return value;
    }

    public String optionalText(String raw, int max) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        if (value.length() > max) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return value;
    }

    public NormalizedResult normalizeResult(RescueResultRequest request) {
        if (request == null || request.getResult() == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String result = request.getResult().trim();
        if ("FAILED".equals(result)) {
            if (request.getAnimals() != null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            return NormalizedResult.failed(requiredText(request.getFailureReason(), 500));
        }
        if (!"SUCCESS".equals(result)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        if (request.getFailureReason() != null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        if (request.getAnimals() == null || request.getAnimals().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        List<NormalizedAnimal> animals = new ArrayList<>();
        Set<String> allTokens = new HashSet<>();
        int totalImages = 0;
        for (AnimalCreateRequest animal : request.getAnimals()) {
            if (animal == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            String name = requiredText(animal.getName(), 100);
            String species = requiredText(animal.getSpecies(), 50);
            String sex = animal.getSex() == null ? null : animal.getSex().trim();
            if (!SEXES.contains(sex)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            Integer age = animal.getEstimatedAgeMonths();
            if (age != null && (age < 0 || age > 65535)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            String color = optionalText(animal.getColor(), 100);
            String healthCondition = requiredText(animal.getHealthCondition(), 1000);
            String personality = optionalText(animal.getPersonality(), 1000);
            String adoptionRequirements = optionalText(animal.getAdoptionRequirements(), 1000);
            String initialHealthRecord = optionalText(animal.getInitialHealthRecord(), 2000);
            List<String> tokens = normalizeTokens(animal.getImageTokens());
            totalImages += tokens.size();
            if (totalImages > 9) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            for (String token : tokens) {
                if (!allTokens.add(token)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            }
            animals.add(new NormalizedAnimal(name, species, sex, age, color, healthCondition, personality,
                    adoptionRequirements, initialHealthRecord, tokens));
        }
        return NormalizedResult.success(animals);
    }

    private List<String> normalizeTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String token : tokens) {
            if (token == null) throw new BusinessException(ErrorCode.INVALID_FILE);
            String value = token.trim();
            try {
                UUID parsed = UUID.fromString(value);
                if (!parsed.toString().equals(value) || !value.equals(value.toLowerCase())) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.INVALID_FILE);
            }
            if (!seen.add(value)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            result.add(value);
        }
        return List.copyOf(result);
    }

    public static class NormalizedResult {
        private final String result;
        private final String failureReason;
        private final List<NormalizedAnimal> animals;
        private NormalizedResult(String result, String failureReason, List<NormalizedAnimal> animals) {
            this.result=result; this.failureReason=failureReason; this.animals=animals;
        }
        public static NormalizedResult failed(String reason){ return new NormalizedResult("FAILED", reason, List.of()); }
        public static NormalizedResult success(List<NormalizedAnimal> animals){ return new NormalizedResult("SUCCESS", null, List.copyOf(animals)); }
        public String getResult(){ return result; }
        public String getFailureReason(){ return failureReason; }
        public List<NormalizedAnimal> getAnimals(){ return animals; }
    }

    public static class NormalizedAnimal {
        private final String name, species, sex, color, healthCondition, personality, adoptionRequirements, initialHealthRecord;
        private final Integer estimatedAgeMonths;
        private final List<String> imageTokens;
        public NormalizedAnimal(String name, String species, String sex, Integer estimatedAgeMonths, String color,
                                String healthCondition, String personality, String adoptionRequirements,
                                String initialHealthRecord, List<String> imageTokens) {
            this.name=name; this.species=species; this.sex=sex; this.estimatedAgeMonths=estimatedAgeMonths;
            this.color=color; this.healthCondition=healthCondition; this.personality=personality;
            this.adoptionRequirements=adoptionRequirements; this.initialHealthRecord=initialHealthRecord;
            this.imageTokens=imageTokens;
        }
        public NormalizedAnimal(String name, String species, String sex, Integer estimatedAgeMonths, String color,
                                String healthCondition, String initialHealthRecord, List<String> imageTokens) {
            this(name,species,sex,estimatedAgeMonths,color,healthCondition,null,null,initialHealthRecord,imageTokens);
        }
        public String getName(){ return name; }
        public String getSpecies(){ return species; }
        public String getSex(){ return sex; }
        public Integer getEstimatedAgeMonths(){ return estimatedAgeMonths; }
        public String getColor(){ return color; }
        public String getHealthCondition(){ return healthCondition; }
        public String getPersonality(){ return personality; }
        public String getAdoptionRequirements(){ return adoptionRequirements; }
        public String getInitialHealthRecord(){ return initialHealthRecord; }
        public List<String> getImageTokens(){ return imageTokens; }
    }
}
