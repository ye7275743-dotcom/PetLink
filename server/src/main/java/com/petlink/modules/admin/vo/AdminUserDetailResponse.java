package com.petlink.modules.admin.vo;

import java.time.OffsetDateTime;

public class AdminUserDetailResponse extends AdminUserSummaryResponse {
    private final AdminUserStatisticsResponse statistics;

    public AdminUserDetailResponse(String id,String account,String nickname,String phone,String roleCode,String status,
                                   OffsetDateTime createdAt,OffsetDateTime updatedAt,AdminUserStatisticsResponse statistics) {
        super(id,account,nickname,phone,roleCode,status,createdAt,updatedAt);this.statistics=statistics;
    }
    public AdminUserStatisticsResponse getStatistics(){return statistics;}
}
