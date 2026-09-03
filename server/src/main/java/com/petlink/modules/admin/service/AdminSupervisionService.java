package com.petlink.modules.admin.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.adoption.service.AdoptionResponseAssembler;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.modules.animal.service.AnimalResponseAssembler;
import com.petlink.modules.animal.vo.AnimalSummaryResponse;
import com.petlink.modules.rescue.service.RescueTaskResponseAssembler;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminSupervisionService {
    private static final Set<String> TASK_STATUSES=Set.of("WAITING_START","IN_PROGRESS","SUCCESS","FAILED","CANCELED");
    private static final Set<String> ANIMAL_STATUSES=Set.of("TREATING","OBSERVING","AVAILABLE","SUSPENDED","ADOPTED");
    private final AdminMapper mapper;
    private final RescueTaskResponseAssembler taskAssembler;
    private final AnimalResponseAssembler animalAssembler;
    private final AdoptionResponseAssembler adoptionAssembler;

    public AdminSupervisionService(AdminMapper mapper,RescueTaskResponseAssembler taskAssembler,
                                   AnimalResponseAssembler animalAssembler,AdoptionResponseAssembler adoptionAssembler) {
        this.mapper=mapper;this.taskAssembler=taskAssembler;this.animalAssembler=animalAssembler;this.adoptionAssembler=adoptionAssembler;
    }

    public PageResponse<TaskSummaryResponse> tasks(UserPrincipal admin,int page,int size,String status,Long rescuerId,Long clueId) {
        AdminUserService.requireAdmin(admin);validatePage(page,size);String state=normalizeStatus(status,TASK_STATUSES);
        validateOptionalId(rescuerId);validateOptionalId(clueId);long offset=(long)(page-1)*size;
        List<TaskSummaryResponse> rows=mapper.selectTasksPage(state,rescuerId,clueId,size,offset).stream()
                .map(taskAssembler::summary).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,mapper.countTasks(state,rescuerId,clueId));
    }

    public PageResponse<AnimalSummaryResponse> animals(UserPrincipal admin,int page,int size,String status,String species,Long rescueTaskId) {
        AdminUserService.requireAdmin(admin);validatePage(page,size);String state=normalizeStatus(status,ANIMAL_STATUSES);
        String normalizedSpecies=normalizeSpecies(species);validateOptionalId(rescueTaskId);long offset=(long)(page-1)*size;
        List<AnimalSummaryResponse> rows=mapper.selectAnimalsPage(state,normalizedSpecies,rescueTaskId,size,offset).stream()
                .map(animalAssembler::summary).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,mapper.countAnimals(state,normalizedSpecies,rescueTaskId));
    }

    public PageResponse<AdoptionRecordResponse> adoptionRecords(UserPrincipal admin,int page,int size,Long userId,Long animalId,Long applicationId) {
        AdminUserService.requireAdmin(admin);validatePage(page,size);validateOptionalId(userId);validateOptionalId(animalId);validateOptionalId(applicationId);
        long offset=(long)(page-1)*size;
        List<AdoptionRecordResponse> rows=mapper.selectAdoptionRecordsPage(userId,animalId,applicationId,size,offset).stream()
                .map(adoptionAssembler::record).collect(Collectors.toList());
        return new PageResponse<>(rows,page,size,mapper.countAdoptionRecords(userId,animalId,applicationId));
    }

    private static void validatePage(int page,int size){if(page<1||size<1||size>100)throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
    private static void validateOptionalId(Long id){if(id!=null&&id<=0)throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
    private static String normalizeStatus(String raw,Set<String> allowed){if(raw==null||raw.isBlank())return null;String v=raw.trim();if(!allowed.contains(v))throw new BusinessException(ErrorCode.INVALID_PARAMETER);return v;}
    private static String normalizeSpecies(String raw){if(raw==null||raw.isBlank())return null;String v=raw.trim();if(v.length()>50)throw new BusinessException(ErrorCode.INVALID_PARAMETER);return v;}
}
