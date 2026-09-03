package com.petlink.modules.content.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.content.entity.Announcement;
import com.petlink.modules.content.mapper.AnnouncementMapper;
import com.petlink.modules.content.vo.AnnouncementAdminDetailResponse;
import com.petlink.modules.content.vo.AnnouncementAdminSummaryResponse;
import com.petlink.modules.content.vo.AnnouncementPublicDetailResponse;
import com.petlink.modules.content.vo.AnnouncementPublicSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnnouncementQueryService {
    private static final Set<String> STATUSES=Set.of("DRAFT","PUBLISHED","WITHDRAWN");
    private final AnnouncementMapper mapper;private final AnnouncementResponseAssembler assembler;
    public AnnouncementQueryService(AnnouncementMapper mapper,AnnouncementResponseAssembler assembler){this.mapper=mapper;this.assembler=assembler;}
    public PageResponse<AnnouncementPublicSummaryResponse> publicList(int page,int size){validatePage(page,size);long offset=(long)(page-1)*size;List<AnnouncementPublicSummaryResponse> rows=mapper.selectPublicPage(size,offset).stream().map(assembler::publicSummary).collect(Collectors.toList());return new PageResponse<>(rows,page,size,mapper.countPublic());}
    public AnnouncementPublicDetailResponse publicDetail(Long id){requireId(id);Announcement a=mapper.selectPublishedById(id);if(a==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return assembler.publicDetail(a);}
    public PageResponse<AnnouncementAdminSummaryResponse> adminList(UserPrincipal p,int page,int size,String status){requireAdmin(p);validatePage(page,size);String s=normalizeStatus(status);long offset=(long)(page-1)*size;List<AnnouncementAdminSummaryResponse> rows=mapper.selectAdminPage(s,size,offset).stream().map(assembler::adminSummary).collect(Collectors.toList());return new PageResponse<>(rows,page,size,mapper.countAdmin(s));}
    public AnnouncementAdminDetailResponse adminDetail(UserPrincipal p,Long id){requireAdmin(p);return assembler.adminDetail(required(id));}
    Announcement required(Long id){requireId(id);Announcement a=mapper.selectById(id);if(a==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return a;}
    static void requireAdmin(UserPrincipal p){if(p==null||!"ADMIN".equals(p.getRoleCode()))throw new BusinessException(ErrorCode.FORBIDDEN);}
    static void requireId(Long id){if(id==null||id<=0)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private void validatePage(int p,int s){if(p<1||s<1||s>100)throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
    private String normalizeStatus(String s){if(s==null||s.isBlank())return null;String v=s.trim();if(!STATUSES.contains(v))throw new BusinessException(ErrorCode.INVALID_PARAMETER);return v;}
}
