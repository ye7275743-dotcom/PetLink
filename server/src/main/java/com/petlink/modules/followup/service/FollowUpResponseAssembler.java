package com.petlink.modules.followup.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.followup.entity.FollowUpImage;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpImageMapper;
import com.petlink.modules.followup.vo.FollowUpImageResponse;
import com.petlink.modules.followup.vo.FollowUpRecordResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FollowUpResponseAssembler {
    private final FollowUpImageMapper imageMapper;
    public FollowUpResponseAssembler(FollowUpImageMapper imageMapper){this.imageMapper=imageMapper;}

    public FollowUpRecordResponse record(FollowUpRecord record){
        List<FollowUpImageResponse> images=imageMapper.selectByFollowUpId(record.getId()).stream().map(this::image).collect(Collectors.toList());
        return new FollowUpRecordResponse(String.valueOf(record.getId()),String.valueOf(record.getAdoptionRecordId()),String.valueOf(record.getSubmitterId()),
                record.getContent(),record.getHealthCondition(),images,TimeUtils.toOffset(record.getCreatedAt()));
    }
    private FollowUpImageResponse image(FollowUpImage image){
        return new FollowUpImageResponse(String.valueOf(image.getId()),"/api/media/follow-up-images/"+image.getId(),image.getSortOrder());
    }
}
