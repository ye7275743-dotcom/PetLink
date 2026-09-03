package com.petlink.modules.clue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class RescueClueMediaService {
    private final RescueClueImageMapper imageMapper;
    private final RescueClueQueryService queryService;
    private final FileStorageService storage;

    public RescueClueMediaService(RescueClueImageMapper imageMapper,
                                  RescueClueQueryService queryService,
                                  FileStorageService storage) {
        this.imageMapper = imageMapper;
        this.queryService = queryService;
        this.storage = storage;
    }

    public FileStorageService.ImageBinary read(UserPrincipal principal, Long imageId) {
        if (imageId == null || imageId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        RescueClueImage image = imageMapper.selectById(imageId);
        if (image == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        queryService.requireVisibleClue(principal, image.getClueId());
        return storage.readImage(image.getImagePath());
    }
}
