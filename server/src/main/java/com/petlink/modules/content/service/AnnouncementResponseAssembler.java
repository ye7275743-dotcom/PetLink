package com.petlink.modules.content.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.content.entity.Announcement;
import com.petlink.modules.content.vo.AnnouncementAdminDetailResponse;
import com.petlink.modules.content.vo.AnnouncementAdminSummaryResponse;
import com.petlink.modules.content.vo.AnnouncementPublicDetailResponse;
import com.petlink.modules.content.vo.AnnouncementPublicSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementResponseAssembler {
    public AnnouncementPublicSummaryResponse publicSummary(Announcement a){return new AnnouncementPublicSummaryResponse(String.valueOf(a.getId()),a.getTitle(),TimeUtils.toOffset(a.getPublishedAt()));}
    public AnnouncementPublicDetailResponse publicDetail(Announcement a){return new AnnouncementPublicDetailResponse(String.valueOf(a.getId()),a.getTitle(),a.getContent(),TimeUtils.toOffset(a.getPublishedAt()));}
    public AnnouncementAdminSummaryResponse adminSummary(Announcement a){return new AnnouncementAdminSummaryResponse(String.valueOf(a.getId()),a.getTitle(),a.getStatus(),TimeUtils.toOffset(a.getPublishedAt()),a.getVersion(),TimeUtils.toOffset(a.getCreatedAt()),TimeUtils.toOffset(a.getUpdatedAt()));}
    public AnnouncementAdminDetailResponse adminDetail(Announcement a){return new AnnouncementAdminDetailResponse(String.valueOf(a.getId()),a.getTitle(),a.getContent(),a.getStatus(),String.valueOf(a.getCreatedBy()),String.valueOf(a.getUpdatedBy()),TimeUtils.toOffset(a.getPublishedAt()),a.getVersion(),TimeUtils.toOffset(a.getCreatedAt()),TimeUtils.toOffset(a.getUpdatedAt()));}
}
