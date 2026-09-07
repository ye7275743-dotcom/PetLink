package com.petlink.modules.content.service;

import com.petlink.common.PageResponse;
import com.petlink.modules.content.dto.AnnouncementVersionRequest;
import com.petlink.modules.content.dto.CreateAnnouncementRequest;
import com.petlink.modules.content.dto.PatchAnnouncementRequest;
import com.petlink.modules.content.vo.AnnouncementAdminDetailResponse;
import com.petlink.modules.content.vo.AnnouncementAdminSummaryResponse;
import com.petlink.modules.content.vo.AnnouncementPublicDetailResponse;
import com.petlink.modules.content.vo.AnnouncementPublicSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {
    private final AnnouncementQueryService query;private final AnnouncementTransactionalService tx;
    public AnnouncementService(AnnouncementQueryService query,AnnouncementTransactionalService tx){this.query=query;this.tx=tx;}
    public PageResponse<AnnouncementPublicSummaryResponse> publicList(int p,int s){return query.publicList(p,s);}
    public AnnouncementPublicDetailResponse publicDetail(Long id){return query.publicDetail(id);}
    public AnnouncementAdminDetailResponse create(UserPrincipal p,CreateAnnouncementRequest r){return tx.create(p,r);}
    public PageResponse<AnnouncementAdminSummaryResponse> adminList(UserPrincipal p,int page,int size,String status){return query.adminList(p,page,size,status);}
    public AnnouncementAdminDetailResponse adminDetail(UserPrincipal p,Long id){return query.adminDetail(p,id);}
    public AnnouncementAdminDetailResponse patch(UserPrincipal p,Long id,PatchAnnouncementRequest r){return tx.patch(p,id,r);}
    public AnnouncementAdminDetailResponse publish(UserPrincipal p,Long id,AnnouncementVersionRequest r){return tx.publish(p,id,r);}
    public void delete(UserPrincipal p,Long id,Integer version){tx.delete(p,id,version);}
    public PageResponse<AnnouncementAdminSummaryResponse> searchAdmin(UserPrincipal p,int page,int size,String status,String keyword){return query.searchAdmin(p,page,size,status,keyword);}
    public AnnouncementAdminDetailResponse withdraw(UserPrincipal p,Long id,AnnouncementVersionRequest r){return tx.withdraw(p,id,r);}
}
