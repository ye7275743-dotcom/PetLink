package com.petlink.modules.animal.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class AnimalAccessService {
    private final AnimalMapper animalMapper;
    private final RescueTaskMapper taskMapper;

    public AnimalAccessService(AnimalMapper animalMapper, RescueTaskMapper taskMapper) {
        this.animalMapper=animalMapper; this.taskMapper=taskMapper;
    }

    public Animal requireVisible(UserPrincipal principal, Long animalId) {
        if (animalId == null || animalId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        Animal animal=animalMapper.selectById(animalId);
        if (animal == null || !isVisible(principal,animal)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return animal;
    }

    public boolean isVisible(UserPrincipal principal, Animal animal) {
        if (animal == null) return false;
        if ("AVAILABLE".equals(animal.getStatus())) return true;
        if (principal == null) return false;
        if ("ADMIN".equals(principal.getRoleCode())) return true;
        return isResponsibleRescuer(principal,animal);
    }

    public boolean isPrivileged(UserPrincipal principal, Animal animal) {
        return principal != null && ("ADMIN".equals(principal.getRoleCode()) || isResponsibleRescuer(principal,animal));
    }

    public boolean isResponsibleRescuer(UserPrincipal principal, Animal animal) {
        if (principal == null || !"RESCUER".equals(principal.getRoleCode()) || animal == null || animal.getRescueTaskId() == null) return false;
        RescueTask task=taskMapper.selectById(animal.getRescueTaskId());
        return task != null && principal.getUserId().equals(task.getRescuerId());
    }

    /** Frozen M04 management permission: Animal is already locked; RescueTask is a plain read only. */
    public void requireManageAccess(UserPrincipal principal, Animal lockedAnimal) {
        if (principal == null || lockedAnimal == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if ("ADMIN".equals(principal.getRoleCode())) return;
        if (isResponsibleRescuer(principal,lockedAnimal)) return;
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
