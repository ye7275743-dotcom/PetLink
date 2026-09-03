package com.petlink.modules.admin.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.vo.AdminUserActionResponse;
import com.petlink.modules.admin.vo.AdminUserDetailResponse;
import com.petlink.modules.admin.vo.AdminUserStatisticsResponse;
import com.petlink.modules.admin.vo.AdminUserSummaryResponse;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {
    private static final Set<String> ROLES=Set.of("USER","RESCUER","ADMIN");
    private static final Set<String> STATUSES=Set.of("ENABLED","DISABLED");
    private final AdminMapper mapper;
    private final AdminResponseAssembler assembler;
    private final OperationLogService logs;

    public AdminUserService(AdminMapper mapper,AdminResponseAssembler assembler,OperationLogService logs) {
        this.mapper=mapper;this.assembler=assembler;this.logs=logs;
    }

    public PageResponse<AdminUserSummaryResponse> list(UserPrincipal admin,int page,int size,String roleCode,String status,String keyword) {
        requireAdmin(admin);validatePage(page,size);
        String role=normalizeEnum(roleCode,ROLES);String state=normalizeEnum(status,STATUSES);String word=normalizeKeyword(keyword);
        long offset=(long)(page-1)*size;
        List<AdminUserSummaryResponse> rows=mapper.selectUsersPage(role,state,word,size,offset).stream()
                .map(assembler::userSummary).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,mapper.countUsers(role,state,word));
    }

    public AdminUserDetailResponse detail(UserPrincipal admin,Long userId) {
        requireAdmin(admin);SysUser user=requiredUser(userId);
        AdminUserStatisticsResponse statistics=new AdminUserStatisticsResponse(mapper.countPublishedClues(userId),
                mapper.countRescueTasks(userId),mapper.countAdoptionApplications(userId),mapper.countAdoptionRecordsByUser(userId));
        return assembler.userDetail(user,statistics);
    }

    @Transactional
    public AdminUserActionResponse enable(UserPrincipal admin,Long userId) {
        requireAdmin(admin);requireId(userId);LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(mapper.enableUser(userId,now)!=1)resolveState(userId,"DISABLED",null);
        logs.append("SYS_USER",userId,"ENABLE","DISABLED","ENABLED",admin.getUserId(),null);
        return assembler.userAction(requiredAfterWrite(userId));
    }

    @Transactional
    public AdminUserActionResponse disable(UserPrincipal admin,Long userId) {
        requireAdmin(admin);requireId(userId);
        if(admin.getUserId().equals(userId))throw new BusinessException(ErrorCode.FORBIDDEN);
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(mapper.disableUser(userId,now)!=1)resolveState(userId,"ENABLED",null);
        logs.append("SYS_USER",userId,"DISABLE","ENABLED","DISABLED",admin.getUserId(),null);
        return assembler.userAction(requiredAfterWrite(userId));
    }

    @Transactional
    public AdminUserActionResponse promoteRescuer(UserPrincipal admin,Long userId) {
        requireAdmin(admin);requireId(userId);LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        if(mapper.promoteRescuer(userId,now)!=1)resolveState(userId,null,"USER");
        logs.append("SYS_USER",userId,"PROMOTE_RESCUER",null,null,admin.getUserId(),null);
        return assembler.userAction(requiredAfterWrite(userId));
    }

    private void resolveState(Long id,String expectedStatus,String expectedRole) {
        SysUser user=mapper.selectUser(id);
        if(user==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(expectedStatus!=null&&!expectedStatus.equals(user.getStatus()))throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        if(expectedRole!=null&&!expectedRole.equals(user.getRoleCode()))throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
    }
    private SysUser requiredUser(Long id){requireId(id);SysUser user=mapper.selectUser(id);if(user==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return user;}
    private SysUser requiredAfterWrite(Long id){SysUser user=mapper.selectUser(id);if(user==null)throw new IllegalStateException("updated sys_user disappeared");return user;}
    private static String normalizeEnum(String raw,Set<String> allowed){if(raw==null||raw.isBlank())return null;String v=raw.trim();if(!allowed.contains(v))throw new BusinessException(ErrorCode.INVALID_PARAMETER);return v;}
    private static String normalizeKeyword(String raw){if(raw==null||raw.isBlank())return null;String v=raw.trim();if(v.length()>50)throw new BusinessException(ErrorCode.INVALID_PARAMETER);return v;}
    private static void validatePage(int page,int size){if(page<1||size<1||size>100)throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
    static void requireAdmin(UserPrincipal p){if(p==null||!"ADMIN".equals(p.getRoleCode()))throw new BusinessException(ErrorCode.FORBIDDEN);}
    static void requireId(Long id){if(id==null||id<=0)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
}
