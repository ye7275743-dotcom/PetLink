package com.petlink.modules.followup.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowUpQueryService {
    private final FollowUpRecordMapper recordMapper;
    private final FollowUpAccessService access;
    private final FollowUpResponseAssembler assembler;

    public FollowUpQueryService(FollowUpRecordMapper recordMapper,FollowUpAccessService access,FollowUpResponseAssembler assembler){
        this.recordMapper=recordMapper;this.access=access;this.assembler=assembler;
    }

    public FollowUpRecordResponse replay(Long submitterId,String key){
        FollowUpRecord record=recordMapper.selectBySubmitterAndKey(submitterId,key);
        if(record==null) throw new IllegalStateException("idempotent follow-up disappeared");
        return assembler.record(record);
    }

    public List<FollowUpRecordResponse> byAdoptionRecord(UserPrincipal principal,Long adoptionRecordId){
        requireId(adoptionRecordId);
        AdoptionRecord adoption=access.visibleAdoptionRecord(principal,adoptionRecordId);
        if(adoption==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return recordMapper.selectByAdoptionRecordAsc(adoptionRecordId).stream().map(assembler::record).collect(Collectors.toList());
    }

    public FollowUpRecordResponse detail(UserPrincipal principal,Long followUpId){
        requireId(followUpId); FollowUpRecord record=recordMapper.selectById(followUpId);
        if(record==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AdoptionRecord adoption=access.visibleAdoptionRecord(principal,record.getAdoptionRecordId());
        if(adoption==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return assembler.record(record);
    }

    public PageResponse<FollowUpRecordResponse> adminPage(UserPrincipal principal,int page,int size,Long animalId,Long userId,Long adoptionRecordId){
        requireAdmin(principal);validatePage(page,size);validateOptionalId(animalId);validateOptionalId(userId);validateOptionalId(adoptionRecordId);
        long offset=(long)(page-1)*size;
        List<FollowUpRecordResponse> rows=recordMapper.selectAdminPage(animalId,userId,adoptionRecordId,size,offset).stream().map(assembler::record).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,recordMapper.countAdmin(animalId,userId,adoptionRecordId));
    }

    public PageResponse<FollowUpRecordResponse> rescuerPage(UserPrincipal principal,int page,int size,Long animalId){
        requireRescuer(principal);validatePage(page,size);validateOptionalId(animalId);
        if(animalId!=null && !access.isResponsibleAnimal(principal,animalId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        long offset=(long)(page-1)*size;
        List<FollowUpRecordResponse> rows=recordMapper.selectRescuerPage(principal.getUserId(),animalId,size,offset).stream().map(assembler::record).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,recordMapper.countRescuer(principal.getUserId(),animalId));
    }

    private void requireAdmin(UserPrincipal p){if(p==null||!"ADMIN".equals(p.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void requireRescuer(UserPrincipal p){if(p==null||!"RESCUER".equals(p.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void validatePage(int page,int size){if(page<1||size<1||size>100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
    private void requireId(Long id){if(id==null||id<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private void validateOptionalId(Long id){if(id!=null&&id<=0) throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
}
