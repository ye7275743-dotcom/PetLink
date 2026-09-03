package com.petlink.modules.adoption.service;

import com.petlink.common.PageResponse;
import com.petlink.modules.adoption.dto.AdoptionAuditRequest;
import com.petlink.modules.adoption.dto.SubmitAdoptionApplicationRequest;
import com.petlink.modules.adoption.vo.AdoptionApplicationDetailResponse;
import com.petlink.modules.adoption.vo.AdoptionApplicationSummaryResponse;
import com.petlink.modules.adoption.vo.AdoptionAuditResponse;
import com.petlink.modules.adoption.vo.AdoptionOverviewResponse;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.modules.adoption.vo.AdoptionStateActionResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class AdoptionService {
    private final AdoptionQueryService query; private final AdoptionTransactionalService tx;
    public AdoptionService(AdoptionQueryService query,AdoptionTransactionalService tx){this.query=query;this.tx=tx;}
    public AdoptionApplicationDetailResponse submit(UserPrincipal p,Long animalId,SubmitAdoptionApplicationRequest r){return tx.submit(p,animalId,r);}
    public PageResponse<AdoptionApplicationSummaryResponse> myApplications(UserPrincipal p,int page,int size,String status){return query.myApplications(p,page,size,status);}
    public AdoptionApplicationDetailResponse applicationDetail(UserPrincipal p,Long id){return query.applicationDetail(p,id);}
    public AdoptionStateActionResponse withdraw(UserPrincipal p,Long id){return tx.withdraw(p,id);}
    public PageResponse<AdoptionApplicationSummaryResponse> adminApplications(UserPrincipal p,int page,int size,String status){return query.adminApplications(p,page,size,status);}
    public AdoptionAuditResponse audit(UserPrincipal p,Long id,AdoptionAuditRequest r){return tx.audit(p,id,r);}
    public PageResponse<AdoptionRecordResponse> myRecords(UserPrincipal p,int page,int size){return query.myRecords(p,page,size);}
    public AdoptionRecordResponse recordDetail(UserPrincipal p,Long id){return query.recordDetail(p,id);}
    public AdoptionOverviewResponse overview(UserPrincipal p,Long animalId){return query.overview(p,animalId);}
}

