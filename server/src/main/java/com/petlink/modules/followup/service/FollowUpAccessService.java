package com.petlink.modules.followup.service;

import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class FollowUpAccessService {
    private final AdoptionRecordMapper adoptionRecordMapper;
    private final AnimalMapper animalMapper;
    private final RescueTaskMapper taskMapper;

    public FollowUpAccessService(AdoptionRecordMapper adoptionRecordMapper,AnimalMapper animalMapper,RescueTaskMapper taskMapper){
        this.adoptionRecordMapper=adoptionRecordMapper;this.animalMapper=animalMapper;this.taskMapper=taskMapper;
    }

    public AdoptionRecord visibleAdoptionRecord(UserPrincipal principal,Long adoptionRecordId){
        if(principal==null||adoptionRecordId==null||adoptionRecordId<=0) return null;
        AdoptionRecord record=adoptionRecordMapper.selectById(adoptionRecordId);
        if(record==null) return null;
        return canRead(principal,record)?record:null;
    }

    public boolean canRead(UserPrincipal principal,AdoptionRecord record){
        if(principal==null||record==null) return false;
        if("ADMIN".equals(principal.getRoleCode())) return true;
        if(principal.getUserId().equals(record.getUserId())) return true;
        if(!"RESCUER".equals(principal.getRoleCode())) return false;
        Animal animal=animalMapper.selectById(record.getAnimalId());
        if(animal==null) return false;
        RescueTask task=taskMapper.selectById(animal.getRescueTaskId());
        return task!=null && principal.getUserId().equals(task.getRescuerId());
    }

    public boolean owns(UserPrincipal principal,AdoptionRecord record){
        return principal!=null && record!=null && principal.getUserId().equals(record.getUserId());
    }

    public boolean isResponsibleAnimal(UserPrincipal principal,Long animalId){
        if(principal==null||!"RESCUER".equals(principal.getRoleCode())||animalId==null||animalId<=0) return false;
        Animal animal=animalMapper.selectById(animalId);
        if(animal==null) return false;
        RescueTask task=taskMapper.selectById(animal.getRescueTaskId());
        return task!=null && principal.getUserId().equals(task.getRescuerId());
    }
}
