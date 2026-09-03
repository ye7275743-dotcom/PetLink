package com.petlink.modules.rescue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.modules.rescue.dto.AddRescueRecordRequest;
import com.petlink.modules.rescue.dto.CancelRescueTaskRequest;
import com.petlink.modules.rescue.dto.FailureResolutionRequest;
import com.petlink.modules.rescue.dto.RescueResultRequest;
import com.petlink.modules.rescue.vo.AcceptTaskResponse;
import com.petlink.modules.rescue.vo.RescueRecordResponse;
import com.petlink.modules.rescue.vo.TaskDetailResponse;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RescueTaskService {
    private final RescueTaskQueryService queryService;
    private final RescueTaskTransactionalService transactionalService;
    private final RescueRequestNormalizer normalizer;
    public RescueTaskService(RescueTaskQueryService queryService, RescueTaskTransactionalService transactionalService,
                             RescueRequestNormalizer normalizer) {
        this.queryService=queryService; this.transactionalService=transactionalService; this.normalizer=normalizer;
    }
    public PageResponse<ClueSummaryResponse> waitingAcceptance(UserPrincipal principal,int page,int size){ return queryService.waitingAcceptance(principal,page,size); }
    public AcceptTaskResponse accept(UserPrincipal principal,Long clueId){ requireRescuer(principal); return transactionalService.accept(principal.getUserId(),clueId); }
    public PageResponse<TaskSummaryResponse> mine(UserPrincipal principal,int page,int size,String status){ return queryService.mine(principal,page,size,status); }
    public TaskDetailResponse detail(UserPrincipal principal,Long taskId){ return queryService.detail(principal,taskId); }
    public StateActionResponse start(UserPrincipal principal,Long taskId){ requireRescuer(principal); return transactionalService.start(principal.getUserId(),taskId); }
    public RescueRecordResponse addRecord(UserPrincipal principal,Long taskId,AddRescueRecordRequest request){
        requireRescuer(principal); return transactionalService.addRecord(principal.getUserId(),taskId,request==null?null:request.getContent());
    }
    public List<RescueRecordResponse> records(UserPrincipal principal,Long taskId){ return queryService.records(principal,taskId); }
    public Object submitResult(UserPrincipal principal,Long taskId,RescueResultRequest request){
        requireRescuer(principal); return transactionalService.submitResult(principal.getUserId(),taskId,normalizer.normalizeResult(request));
    }
    public StateActionResponse cancel(UserPrincipal principal,Long taskId,CancelRescueTaskRequest request){
        requireAdmin(principal); return transactionalService.cancel(principal.getUserId(),taskId,request);
    }
    public StateActionResponse resolveFailure(UserPrincipal principal,Long taskId,FailureResolutionRequest request){
        requireAdmin(principal); return transactionalService.resolveFailure(principal.getUserId(),taskId,request);
    }
    private void requireRescuer(UserPrincipal p){ if(p==null||!"RESCUER".equals(p.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN); }
    private void requireAdmin(UserPrincipal p){ if(p==null||!"ADMIN".equals(p.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN); }
}
