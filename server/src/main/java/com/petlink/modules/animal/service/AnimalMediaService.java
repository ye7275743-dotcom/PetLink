package com.petlink.modules.animal.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class AnimalMediaService {
    private final AnimalImageMapper imageMapper;
    private final AnimalMapper animalMapper;
    private final AnimalAccessService access;
    private final AdoptionRecordMapper adoptionRecordMapper;
    private final FileStorageService storage;

    public AnimalMediaService(AnimalImageMapper imageMapper, AnimalMapper animalMapper, AnimalAccessService access,
                              AdoptionRecordMapper adoptionRecordMapper, FileStorageService storage) {
        this.imageMapper=imageMapper; this.animalMapper=animalMapper; this.access=access;
        this.adoptionRecordMapper=adoptionRecordMapper; this.storage=storage;
    }

    public FileStorageService.ImageBinary read(UserPrincipal principal,Long imageId) {
        if (imageId == null || imageId<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        AnimalImage image=imageMapper.selectById(imageId);
        if (image == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        Animal animal=animalMapper.selectById(image.getAnimalId());
        if (animal == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        boolean visible=access.isVisible(principal,animal);
        boolean finalAdopter=principal != null && adoptionRecordMapper.existsByAnimalAndUser(animal.getId(),principal.getUserId())==1;
        if (!visible && !finalAdopter) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return storage.readImage(image.getImagePath());
    }
}
