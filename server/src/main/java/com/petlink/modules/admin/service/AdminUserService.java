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
import com.petlink.modules.admin.dto.ChangeRoleRequest;
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

    @Transactional
    public void deleteUser(UserPrincipal admin,Long userId) {
        requireAdmin(admin);requireId(userId);
        SysUser user=mapper.lockUser(userId);
        if(user==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if("ADMIN".equals(user.getRoleCode())||admin.getUserId().equals(userId))throw new BusinessException(ErrorCode.FORBIDDEN,"不能删除管理员或当前登录账号");
        if(mapper.countActiveTasks(userId)>0)throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT,"该用户还有未完成救助任务，请先完成或处置任务");
        if(mapper.softDeleteUser(userId,LocalDateTime.now(TimeUtils.ZONE))!=1)throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        logs.append("SYS_USER",userId,"DELETE_USER",user.getStatus(),"DELETED",admin.getUserId(),"管理员确认删除账号，保留历史业务关联");
    }

    @Transactional
    public AdminUserActionResponse changeRole(UserPrincipal admin,Long userId,ChangeRoleRequest request) {
        requireAdmin(admin);requireId(userId);
        if(request==null || !Set.of("USER","RESCUER").contains(request.getRoleCode()==null?"":request.getRoleCode()))
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String reason=request.getReason()==null?"":request.getReason().trim();
        if(reason.isEmpty() || reason.length()>500)throw new BusinessException(ErrorCode.INVALID_PARAMETER,"请填写 1～500 字的角色调整原因");
        // Serialize with rescue acceptance on the account row; preserve historical ownership.
        SysUser user=mapper.lockUser(userId);
        if(user==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if("ADMIN".equals(user.getRoleCode()) || admin.getUserId().equals(userId))throw new BusinessException(ErrorCode.FORBIDDEN);
        if(user.getRoleCode().equals(request.getRoleCode()))throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT,"该账号已是目标角色，请刷新列表");
        if("USER".equals(request.getRoleCode()) && mapper.countActiveTasks(userId)>0)
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT,"该救助人员仍有待开始或进行中的任务，请先完成或处置任务后再降级");
        String previous=user.getRoleCode();
        if(mapper.changeRole(userId,previous,request.getRoleCode(),LocalDateTime.now(TimeUtils.ZONE))!=1)
            throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        logs.append("SYS_USER",userId,"USER".equals(request.getRoleCode())?"DEMOTE_RESCUER":"PROMOTE_RESCUER",previous,request.getRoleCode(),admin.getUserId(),reason);
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
