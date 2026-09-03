package com.petlink.modules.followup.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.followup.entity.FollowUpImage;
import com.petlink.modules.followup.entity.FollowUpRecord;
import com.petlink.modules.followup.mapper.FollowUpImageMapper;
import com.petlink.modules.followup.mapper.FollowUpRecordMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class FollowUpMediaService {
    private final FollowUpImageMapper imageMapper;
    private final FollowUpRecordMapper recordMapper;
    private final FollowUpAccessService access;
    private final FileStorageService storage;

    public FollowUpMediaService(FollowUpImageMapper imageMapper,FollowUpRecordMapper recordMapper,FollowUpAccessService access,FileStorageService storage){
        this.imageMapper=imageMapper;this.recordMapper=recordMapper;this.access=access;this.storage=storage;
    }

    public FileStorageService.ImageBinary read(UserPrincipal principal,Long imageId){
        if(principal==null||imageId==null||imageId<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        FollowUpImage image=imageMapper.selectById(imageId); if(image==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        FollowUpRecord record=recordMapper.selectById(image.getFollowUpId()); if(record==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AdoptionRecord adoption=access.visibleAdoptionRecord(principal,record.getAdoptionRecordId());
        if(adoption==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return storage.readImage(image.getImagePath());
    }
}
