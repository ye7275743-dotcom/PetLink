package com.petlink.modules.admin.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.admin.vo.AdminUserActionResponse;
import com.petlink.modules.admin.vo.AdminUserDetailResponse;
import com.petlink.modules.admin.vo.AdminUserStatisticsResponse;
import com.petlink.modules.admin.vo.AdminUserSummaryResponse;
import com.petlink.modules.auth.entity.SysUser;
import org.springframework.stereotype.Component;

@Component
public class AdminResponseAssembler {
    public AdminUserSummaryResponse userSummary(SysUser user) {
        return new AdminUserSummaryResponse(String.valueOf(user.getId()),user.getAccount(),user.getNickname(),user.getPhone(),
                user.getRoleCode(),user.getStatus(),TimeUtils.toOffset(user.getCreatedAt()),TimeUtils.toOffset(user.getUpdatedAt()));
    }

    public AdminUserDetailResponse userDetail(SysUser user,AdminUserStatisticsResponse statistics) {
        AdminUserSummaryResponse base=userSummary(user);
        return new AdminUserDetailResponse(base.getId(),base.getAccount(),base.getNickname(),base.getPhone(),base.getRoleCode(),
                base.getStatus(),base.getCreatedAt(),base.getUpdatedAt(),statistics);
    }

    public AdminUserActionResponse userAction(SysUser user) {
        return new AdminUserActionResponse(String.valueOf(user.getId()),user.getRoleCode(),user.getStatus(),TimeUtils.toOffset(user.getUpdatedAt()));
    }
}
