package com.petlink.modules.animal.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.vo.AnimalDetailResponse;
import com.petlink.modules.animal.vo.AnimalSummaryResponse;
import com.petlink.modules.animal.vo.HealthRecordPublicResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AnimalQueryService {
    private static final Set<String> STATUSES=Set.of("TREATING","OBSERVING","AVAILABLE","SUSPENDED","ADOPTED");
    private static final Set<String> SEXES=Set.of("MALE","FEMALE","UNKNOWN");
    private final AnimalMapper animalMapper;
    private final AnimalAccessService access;
    private final AnimalResponseAssembler assembler;

    public AnimalQueryService(AnimalMapper animalMapper, AnimalAccessService access, AnimalResponseAssembler assembler) {
        this.animalMapper=animalMapper; this.access=access; this.assembler=assembler;
    }

    public PageResponse<AnimalSummaryResponse> publicList(int page,int size,String species,String sex) {
        validatePage(page,size);
        String normalizedSpecies=normalizeOptionalText(species,50);
        String normalizedSex=normalizeSex(sex);
        long offset=(long)(page-1)*size;
        List<AnimalSummaryResponse> records=assembler.summaries(animalMapper.selectPublicPage(normalizedSpecies,normalizedSex,size,offset));
        return new PageResponse<>(records,page,size,animalMapper.countPublic(normalizedSpecies,normalizedSex));
    }

    public AnimalDetailResponse detail(UserPrincipal principal,Long animalId) {
        Animal animal=access.requireVisible(principal,animalId);
        return assembler.detail(animal,access.isPrivileged(principal,animal));
    }

    public PageResponse<AnimalSummaryResponse> responsible(UserPrincipal principal,int page,int size,String status) {
        if (principal == null || !"RESCUER".equals(principal.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);
        validatePage(page,size);
        String normalized=normalizeStatus(status);
        long offset=(long)(page-1)*size;
        List<AnimalSummaryResponse> records=assembler.summaries(animalMapper.selectResponsiblePage(principal.getUserId(),normalized,size,offset));
        return new PageResponse<>(records,page,size,animalMapper.countResponsible(principal.getUserId(),normalized));
    }

    public List<HealthRecordPublicResponse> healthRecords(UserPrincipal principal,Long animalId) {
        Animal animal=access.requireVisible(principal,animalId);
        return assembler.healthRecords(animal.getId(),access.isPrivileged(principal,animal));
    }

    private void validatePage(int page,int size) {
        if (page<1 || size<1 || size>100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String v=status.trim(); if (!STATUSES.contains(v)) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;
    }
    private String normalizeSex(String sex) {
        if (sex == null || sex.isBlank()) return null;
        String v=sex.trim(); if (!SEXES.contains(v)) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;
    }
    private String normalizeOptionalText(String text,int max) {
        if (text == null || text.isBlank()) return null;
        String v=text.trim(); if (v.length()>max) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return v;
    }
}
