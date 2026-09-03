package com.petlink.modules.followup.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.modules.followup.service.FollowUpRequestNormalizer.NormalizedSubmit;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpTransactionalService {
    private final AdoptionRecordMapper adoptionRecordMapper;
    private final FollowUpRecordMapper recordMapper;
    private final FollowUpFileBindingService files;
    private final FollowUpResponseAssembler assembler;

    public FollowUpTransactionalService(AdoptionRecordMapper adoptionRecordMapper,FollowUpRecordMapper recordMapper,
                                        FollowUpFileBindingService files,FollowUpResponseAssembler assembler){
        this.adoptionRecordMapper=adoptionRecordMapper;this.recordMapper=recordMapper;this.files=files;this.assembler=assembler;
    }

    @Transactional
    public FollowUpRecordResponse create(UserPrincipal principal,Long adoptionRecordId,NormalizedSubmit request){
        AdoptionRecord adoption=adoptionRecordMapper.selectById(adoptionRecordId);
        if(adoption==null||!principal.getUserId().equals(adoption.getUserId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        FollowUpRecord record=new FollowUpRecord();
        record.setAdoptionRecordId(adoptionRecordId);record.setSubmitterId(principal.getUserId());record.setContent(request.getContent());
        record.setHealthCondition(request.getHealthCondition());record.setIdempotencyKey(request.getIdempotencyKey());
        if(recordMapper.insert(record)!=1) throw new IllegalStateException("follow_up_record insert affected rows != 1");

        FollowUpFileBindingService.PreparedBindings prepared=files.lockAndValidate(principal.getUserId(),request.getImageTokens());
        files.bindPrepared(principal.getUserId(),record.getId(),request.getImageTokens(),prepared);
        files.copyPrepared(prepared);

        FollowUpRecord saved=recordMapper.selectById(record.getId());
        if(saved==null) throw new IllegalStateException("new follow_up_record cannot be reloaded");
        return assembler.record(saved);
    }
}
