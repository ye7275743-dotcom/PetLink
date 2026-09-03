package com.petlink.modules.followup.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.followup.dto.SubmitFollowUpRequest;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class FollowUpService {
    private final FollowUpRecordMapper recordMapper;
    private final FollowUpRequestNormalizer normalizer;
    private final FollowUpTransactionalService tx;
    private final FollowUpQueryService query;

    public FollowUpService(FollowUpRecordMapper recordMapper,FollowUpRequestNormalizer normalizer,
                           FollowUpTransactionalService tx,FollowUpQueryService query){
        this.recordMapper=recordMapper;this.normalizer=normalizer;this.tx=tx;this.query=query;
    }

    public SubmissionResult submit(UserPrincipal principal,Long adoptionRecordId,SubmitFollowUpRequest request){
        requireSubmitter(principal);requireId(adoptionRecordId);
        String key=normalizer.normalizeIdempotencyKey(request);

        FollowUpRecord existing=recordMapper.selectBySubmitterAndKey(principal.getUserId(),key);
        if(existing!=null) return replayOrConflict(existing,adoptionRecordId);

        FollowUpRequestNormalizer.NormalizedSubmit normalized=normalizer.normalizeForCreate(request,key);
        try {
            return new SubmissionResult(tx.create(principal,adoptionRecordId,normalized),true);
        } catch(DataIntegrityViolationException ex){
            if(!hasConstraint(ex,"uk_follow_up_submitter_idempotency")) throw ex;
            FollowUpRecord concurrent=recordMapper.selectBySubmitterAndKey(principal.getUserId(),key);
            if(concurrent==null) throw ex;
            return replayOrConflict(concurrent,adoptionRecordId);
        }
    }

    public List<FollowUpRecordResponse> byAdoptionRecord(UserPrincipal p,Long id){return query.byAdoptionRecord(p,id);}
    public FollowUpRecordResponse detail(UserPrincipal p,Long id){return query.detail(p,id);}
    public PageResponse<FollowUpRecordResponse> adminPage(UserPrincipal p,int page,int size,Long animalId,Long userId,Long adoptionRecordId){return query.adminPage(p,page,size,animalId,userId,adoptionRecordId);}
    public PageResponse<FollowUpRecordResponse> rescuerPage(UserPrincipal p,int page,int size,Long animalId){return query.rescuerPage(p,page,size,animalId);}

    private SubmissionResult replayOrConflict(FollowUpRecord existing,Long adoptionRecordId){
        if(!Objects.equals(existing.getAdoptionRecordId(),adoptionRecordId)) throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        return new SubmissionResult(query.replay(existing.getSubmitterId(),existing.getIdempotencyKey()),false);
    }
    private void requireSubmitter(UserPrincipal p){if(p==null||!("USER".equals(p.getRoleCode())||"RESCUER".equals(p.getRoleCode()))) throw new BusinessException(ErrorCode.FORBIDDEN);}
    private void requireId(Long id){if(id==null||id<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private boolean hasConstraint(Throwable ex,String name){String needle=name.toLowerCase(Locale.ROOT);for(Throwable t=ex;t!=null;t=t.getCause()){String m=t.getMessage();if(m!=null&&m.toLowerCase(Locale.ROOT).contains(needle)) return true;}return false;}

    public static class SubmissionResult {
        private final FollowUpRecordResponse response; private final boolean created;
        public SubmissionResult(FollowUpRecordResponse response,boolean created){this.response=response;this.created=created;}
        public FollowUpRecordResponse getResponse(){return response;}
        public boolean isCreated(){return created;}
    }
}
